package org.example.nura.domain.schedule.service.plan;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.home.service.HomeAiCommentService;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator.DailyPlanSummary;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPlanSaveService {

    private final UserRepository userRepository;

    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;

    private final DailyPlanSummaryCalculator dailyPlanSummaryCalculator;
    private final HomeAiCommentService homeAiCommentService;

    @Transactional
    public DailyTimeAllocation save(
            Long userId,
            DailyPlanContext context,
            List<PlannedTimeBlock> plannedBlocks
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.RESOURCE_NOT_FOUND
                        )
                );

        // 총 시간 계산
        DailyPlanSummary summary =
                dailyPlanSummaryCalculator.calculate(
                        plannedBlocks
                );

        // 홈 ai 코멘트 생성
        String aiComment =
                homeAiCommentService.generate(
                        context,
                        summary,
                        plannedBlocks
                );

        // 하루 시간 배분 저장
        DailyTimeAllocation allocation =
                DailyTimeAllocation.create(
                        user,
                        context.date(),
                        summary.socialMinutes(),
                        summary.refreshMinutes(),
                        summary.myMinutes(),
                        aiComment
                );

        dailyTimeAllocationRepository.save(
                allocation
        );

        // 실제 타임 블록 저장
        List<TimeBlock> timeBlocks =
                plannedBlocks.stream()
                        .map(block ->
                                toTimeBlock(
                                        allocation,
                                        block
                                )
                        )
                        .toList();

        timeBlockRepository.saveAll(
                timeBlocks
        );

        return allocation;
    }

    private TimeBlock toTimeBlock(
            DailyTimeAllocation allocation,
            PlannedTimeBlock block
    ) {
        return TimeBlock.create(
                allocation,
                block.customEvent(),
                block.category(),
                block.label(),
                block.startAt(),
                block.endAt(),
                block.source(),
                null
        );
    }
}
