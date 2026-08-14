package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.request.CustomEventCheckRequest;
import org.example.nura.domain.schedule.dto.response.CustomEventCheckResponse;
import org.example.nura.domain.schedule.dto.response.CustomEventCheckStatus;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.service.plan.DailyPlanContextReader;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomEventService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final DailyPlanContextReader dailyPlanContextReader;

    public CustomEventCheckResponse check(
            Long userId,
            CustomEventCheckRequest request
    ) {
        validateTodayOnly(
                request
        );

        LocalDate today =
                LocalDate.now(KST);

        DailyTimeAllocation allocation =
                dailyTimeAllocationRepository
                        .findByUserIdAndDate(
                                userId,
                                today
                        )
                        .orElseThrow(() ->
                                new BaseException(
                                        ErrorCode.RESOURCE_NOT_FOUND,
                                        "오늘 생성된 시간 설계가 없습니다."
                                )
                        );

        List<TimeBlock> blocks =
                timeBlockRepository
                        .findAllByAllocationIdOrderByStartAtAsc(
                                allocation.getId()
                        );

        int currentRefreshMinutes =
                calculateCurrentRefreshMinutes(
                        blocks
                );

        int expectedRefreshMinutes =
                calculateExpectedRefreshMinutes(
                        blocks,
                        request
                );

        int refreshDecreaseMinutes =
                Math.max(
                        0,
                        currentRefreshMinutes
                                - expectedRefreshMinutes
                );

        DailyPlanContext context =
                dailyPlanContextReader.read(
                        userId,
                        today
                );

        int minimumRecommendedRefreshMinutes =
                calculateMinimumRecommendedRefreshMinutes(
                        context
                );

        boolean belowMinimumRecommended =
                expectedRefreshMinutes
                        < minimumRecommendedRefreshMinutes;

        if (refreshDecreaseMinutes == 0) {
            return new CustomEventCheckResponse(
                    CustomEventCheckStatus.AVAILABLE,
                    0,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new CustomEventCheckResponse(
                CustomEventCheckStatus.REFRESH_REDUCED,
                refreshDecreaseMinutes,
                currentRefreshMinutes,
                expectedRefreshMinutes,
                minimumRecommendedRefreshMinutes,
                belowMinimumRecommended
        );
    }

    private void validateTodayOnly(
            CustomEventCheckRequest request
    ) {
        if (request.startAt() == null
                || request.endAt() == null
                || request.category() == null
                || request.eventName() == null
                || request.eventName().isBlank()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 입력값이 올바르지 않습니다."
            );
        }

        if (!request.endAt().isAfter(request.startAt())) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 종료 시간은 시작 시간보다 이후여야 합니다."
            );
        }

        LocalDate today =
                LocalDate.now(KST);

        if (!request.startAt().toLocalDate().equals(today)
                || !request.endAt().toLocalDate().equals(today)) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 추가는 오늘 날짜만 가능합니다."
            );
        }
    }

    private int calculateCurrentRefreshMinutes(
            List<TimeBlock> blocks
    ) {
        return blocks.stream()
                .filter(block ->
                        block.getCategory() == TimeCategory.REFRESH
                )
                .mapToInt(block ->
                        (int) Duration.between(
                                block.getStartAt(),
                                block.getEndAt()
                        ).toMinutes()
                )
                .sum();
    }

    private int calculateExpectedRefreshMinutes(
            List<TimeBlock> blocks,
            CustomEventCheckRequest request
    ) {
        int refreshMinutes =
                calculateCurrentRefreshMinutes(
                        blocks
                );

        for (TimeBlock block : blocks) {
            LocalDateTime overlapStart =
                    max(
                            block.getStartAt(),
                            request.startAt()
                    );

            LocalDateTime overlapEnd =
                    min(
                            block.getEndAt(),
                            request.endAt()
                    );

            if (!overlapStart.isBefore(overlapEnd)) {
                continue;
            }

            int overlapMinutes =
                    (int) Duration.between(
                            overlapStart,
                            overlapEnd
                    ).toMinutes();

            if (block.getCategory() == TimeCategory.REFRESH
                    && request.category() != TimeCategory.REFRESH) {
                refreshMinutes -= overlapMinutes;
            } else if (block.getCategory() != TimeCategory.REFRESH
                    && request.category() == TimeCategory.REFRESH) {
                refreshMinutes += overlapMinutes;
            }
        }

        return refreshMinutes;
    }

    private int calculateMinimumRecommendedRefreshMinutes(
            DailyPlanContext context
    ) {
        int targetSleepMinutes =
                context.targetSleepMinutes() == null
                        ? 0
                        : context.targetSleepMinutes();

        return targetSleepMinutes
                + resolveMealMinutes(
                context.mealPattern()
        );
    }

    private int resolveMealMinutes(
            MealPattern mealPattern
    ) {
        if (mealPattern == null) {
            return 60;
        }

        return switch (mealPattern) {
            case REGULAR, SOMETIMES_SKIP -> 60;
            case OFTEN_SKIP -> 30;
        };
    }

    private LocalDateTime max(
            LocalDateTime left,
            LocalDateTime right
    ) {
        return left.isAfter(right)
                ? left
                : right;
    }

    private LocalDateTime min(
            LocalDateTime left,
            LocalDateTime right
    ) {
        return left.isBefore(right)
                ? left
                : right;
    }
}
