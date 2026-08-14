package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.request.CustomEventCheckRequest;
import org.example.nura.domain.schedule.dto.response.CustomEventCreateResponse;
import org.example.nura.domain.schedule.dto.response.CustomEventCheckResponse;
import org.example.nura.domain.schedule.dto.response.CustomEventCheckStatus;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.entity.CustomEvent;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.domain.schedule.repository.CustomEventRepository;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.service.overlay.CustomEventOverlayService;
import org.example.nura.domain.schedule.service.overlay.TimeBlockOverlayService;
import org.example.nura.domain.schedule.service.plan.DailyPlanContextReader;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator.DailyPlanSummary;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomEventService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final CustomEventRepository customEventRepository;
    private final UserRepository userRepository;
    private final DailyPlanContextReader dailyPlanContextReader;
    private final DailyPlanSummaryCalculator dailyPlanSummaryCalculator;
    private final CustomEventOverlayService customEventOverlayService;
    private final TimeBlockOverlayService timeBlockOverlayService;

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

    @Transactional
    public CustomEventCreateResponse create(
            Long userId,
            CustomEventCheckRequest request
    ) {
        validateCreateRequest(
                request
        );

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new BaseException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        CustomEvent customEvent =
                CustomEvent.create(
                        user,
                        request.category(),
                        request.eventName(),
                        request.startAt(),
                        request.endAt()
                );

        List<CustomEvent> overlappingEvents =
                customEventRepository.findOverlappingEvents(
                        userId,
                        request.startAt(),
                        request.endAt()
                );

        CustomEventOverlayService.OverlayResult customEventOverlayResult =
                customEventOverlayService.overlay(
                        overlappingEvents,
                        customEvent
                );

        customEventRepository.save(
                customEvent
        );

        if (!customEventOverlayResult.eventsToInsert().isEmpty()) {
            customEventRepository.saveAll(
                    customEventOverlayResult.eventsToInsert()
            );
        }

        LocalDate today =
                LocalDate.now(KST);

        if (!request.startAt().toLocalDate().equals(today)
                || !request.endAt().toLocalDate().equals(today)) {
            if (!customEventOverlayResult.eventsToDelete().isEmpty()) {
                customEventRepository.deleteAll(
                        customEventOverlayResult.eventsToDelete()
                );
            }

            return new CustomEventCreateResponse(
                    customEvent.getId(),
                    customEvent.getCategory(),
                    customEvent.getEventName(),
                    customEvent.getStartAt(),
                    customEvent.getEndAt()
            );
        }

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

        List<TimeBlock> existingBlocks =
                timeBlockRepository
                        .findAllByAllocationIdOrderByStartAtAsc(
                                allocation.getId()
                        );

        TimeBlockOverlayService.OverlayResult overlayResult =
                timeBlockOverlayService.overlay(
                        allocation,
                        existingBlocks,
                        request.category(),
                        request.eventName(),
                        request.startAt(),
                        request.endAt(),
                        customEvent,
                        customEventOverlayResult.rightSplitEventsByOriginalId()
                );

        validateOverlayResult(
                overlayResult.finalBlocks()
        );

        if (!overlayResult.blocksToDelete().isEmpty()) {
            timeBlockRepository.deleteAll(
                    overlayResult.blocksToDelete()
            );
        }

        if (!overlayResult.blocksToInsert().isEmpty()) {
            timeBlockRepository.saveAll(
                    overlayResult.blocksToInsert()
            );
        }

        if (!customEventOverlayResult.eventsToDelete().isEmpty()) {
            customEventRepository.deleteAll(
                    customEventOverlayResult.eventsToDelete()
            );
        }

        DailyPlanSummary summary =
                dailyPlanSummaryCalculator.calculate(
                        toPlannedBlocks(
                                overlayResult.finalBlocks()
                        )
                );

        allocation.updateAllocation(
                summary.socialMinutes(),
                summary.refreshMinutes(),
                summary.myMinutes(),
                allocation.getAiComment()
        );

        return new CustomEventCreateResponse(
                customEvent.getId(),
                customEvent.getCategory(),
                customEvent.getEventName(),
                customEvent.getStartAt(),
                customEvent.getEndAt()
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

    private void validateCreateRequest(
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

    private List<PlannedTimeBlock> toPlannedBlocks(
            List<TimeBlock> blocks
    ) {
        return blocks.stream()
                .map(block ->
                        new PlannedTimeBlock(
                                block.getCategory(),
                                block.getLabel(),
                                block.getStartAt(),
                                block.getEndAt(),
                                block.getSource(),
                                block.getCustomEvent()
                        )
                )
                .toList();
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

    private void validateOverlayResult(
            List<TimeBlock> blocks
    ) {
        if (blocks.isEmpty()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 반영 결과가 비어 있습니다."
            );
        }

        List<TimeBlock> sorted =
                new ArrayList<>(blocks);
        sorted.sort(
                Comparator.comparing(
                        TimeBlock::getStartAt
                )
        );

        LocalDateTime cursor =
                sorted.get(0).getStartAt();

        if (!cursor.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 반영 결과가 하루 시작과 일치하지 않습니다."
            );
        }

        long totalMinutes = 0;

        for (TimeBlock block : sorted) {
            if (block.getStartAt().isBefore(cursor)) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "일정 반영 결과에 겹치는 시간이 있습니다."
                );
            }

            if (block.getStartAt().isAfter(cursor)) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "일정 반영 결과에 빈 시간이 있습니다."
                );
            }

            totalMinutes += Duration.between(
                    block.getStartAt(),
                    block.getEndAt()
            ).toMinutes();

            cursor =
                    block.getEndAt();
        }

        if (!cursor.toLocalDate().equals(sorted.get(0).getStartAt().toLocalDate().plusDays(1))
                || !cursor.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 반영 결과가 하루 종료와 일치하지 않습니다."
            );
        }

        if (totalMinutes != 24 * 60) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "일정 반영 결과의 총 시간이 1440분이 아닙니다."
            );
        }
    }

}
