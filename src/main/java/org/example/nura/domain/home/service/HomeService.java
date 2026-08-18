package org.example.nura.domain.home.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.home.dto.response.HomeBadgeResponse;
import org.example.nura.domain.home.dto.response.HomeBlockResponse;
import org.example.nura.domain.home.dto.response.HomeResponse;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.service.plan.DailyPlanContextReader;
import org.example.nura.domain.schedule.service.plan.DailyPlanGenerationService;
import org.example.nura.domain.schedule.service.plan.DailyPlanSaveService;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator.DailyPlanSummary;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserRepository userRepository;

    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final DutyScheduleRepository dutyScheduleRepository;

    private final DailyPlanContextReader dailyPlanContextReader;
    private final DailyPlanGenerationService dailyPlanGenerationService;
    private final DailyPlanSaveService dailyPlanSaveService;
    private final DailyPlanSummaryCalculator dailyPlanSummaryCalculator;
    private final HomeAiCommentService homeAiCommentService;

    private final HomeBadgeGenerator homeBadgeGenerator;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public HomeResponse getToday(
            Long userId
    ) {
        LocalDate today =
                LocalDate.now(KST);

        DailyTimeAllocation allocation =
                dailyTimeAllocationRepository
                        .findByUserIdAndDate(
                                userId,
                                today
                        )
                        .orElse(null);

        if (allocation == null) {
            if (!dutyScheduleRepository.existsByUserIdAndDate(
                    userId,
                    today
            )) {
                return createNoScheduleHomeResponse(
                        userId,
                        today
                );
            }

            throw new BaseException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "오늘 생성된 시간 설계가 없습니다."
            );
        }

        return toResponse(
                userId,
                today,
                allocation
        );
    }

    @Transactional
    public HomeResponse ensureToday(
            Long userId
    ) {
        LocalDate today =
                LocalDate.now(KST);

        DailyTimeAllocation existing =
                dailyTimeAllocationRepository
                        .findByUserIdAndDate(
                                userId,
                                today
                        )
                        .orElse(null);

        // 이미 생성된 경우 그대로 조회
        if (existing != null) {
            return toResponse(
                    userId,
                    today,
                    existing
            );
        }

        if (!dutyScheduleRepository.existsByUserIdAndDate(userId, today)) {
            return createNoScheduleHomeResponse(
                    userId,
                    today
            );
        }

        // 오늘 설계에 필요한 컨텍스트 수집
        DailyPlanContext context =
                dailyPlanContextReader.read(
                        userId,
                        today
                );

        // 오늘 시간표 생성
        List<PlannedTimeBlock> plannedBlocks =
                dailyPlanGenerationService.generate(
                        userId,
                        context
                );

        // AI 코멘트 먼저 생성 (트랜잭션 밖)
        DailyPlanSummary summary =
                dailyPlanSummaryCalculator.calculate(
                        plannedBlocks
                );

        String aiComment =
                homeAiCommentService.generate(
                        context,
                        summary,
                        plannedBlocks
                );

        // DB 저장 (트랜잭션 내, LLM 호출 없음)
        DailyTimeAllocation allocation;
        try {
            allocation =
                    dailyPlanSaveService.save(
                            userId,
                            context,
                            plannedBlocks,
                            aiComment
                    );
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동시 요청으로 이미 생성된 경우 기존 설계를 반환
            allocation =
                    dailyTimeAllocationRepository
                            .findByUserIdAndDate(
                                    userId,
                                    today
                            )
                            .orElseThrow(() -> e);
        }

        return toResponse(
                userId,
                today,
                allocation,
                context
        );
    }

    private HomeResponse createNoScheduleHomeResponse(
            Long userId,
            LocalDate today
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        return new HomeResponse(
                today,
                user.getNickname(),
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private HomeResponse toResponse(
            Long userId,
            LocalDate date,
            DailyTimeAllocation allocation
    ) {
        DailyPlanContext context =
                dailyPlanContextReader.read(
                        userId,
                        date
                );

        return toResponse(
                userId,
                date,
                allocation,
                context
        );
    }

    private HomeResponse toResponse(
            Long userId,
            LocalDate date,
            DailyTimeAllocation allocation,
            DailyPlanContext context
    ) {
        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new BaseException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        ShiftType shiftType =
                dutyScheduleRepository
                        .findByUserIdAndDate(
                                userId,
                                date
                        )
                        .map(DutySchedule::getShiftType)
                        .orElse(null);

        List<TimeBlock> timeBlocks =
                timeBlockRepository
                        .findAllByAllocationIdOrderByStartAtAsc(
                                allocation.getId()
                        );

        List<HomeBlockResponse> blockResponses =
                timeBlocks.stream()
                        .map(HomeBlockResponse::from)
                        .toList();

        List<HomeBadgeResponse> badges =
                homeBadgeGenerator.generate(
                        context
                );

        return new HomeResponse(
                date,
                user.getNickname(),
                shiftType,

                allocation.getSocialTime(),
                allocation.getRefreshTime(),
                allocation.getMyTime(),

                allocation.getAiComment(),

                badges,
                blockResponses
        );
    }
}