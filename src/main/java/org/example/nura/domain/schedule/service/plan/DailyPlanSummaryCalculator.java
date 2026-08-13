package org.example.nura.domain.schedule.service.plan;

import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class DailyPlanSummaryCalculator {

    public DailyPlanSummary calculate(
            List<PlannedTimeBlock> blocks
    ) {
        int socialMinutes =
                calculateMinutes(
                        blocks,
                        TimeCategory.SOCIAL
                );

        int refreshMinutes =
                calculateMinutes(
                        blocks,
                        TimeCategory.REFRESH
                );

        int myMinutes =
                calculateMinutes(
                        blocks,
                        TimeCategory.MY
                );

        return new DailyPlanSummary(
                socialMinutes,
                refreshMinutes,
                myMinutes
        );
    }

    private int calculateMinutes(
            List<PlannedTimeBlock> blocks,
            TimeCategory category
    ) {
        return blocks.stream()
                .filter(block ->
                        block.category() == category
                )
                .mapToInt(block ->
                        (int) Duration.between(
                                block.startAt(),
                                block.endAt()
                        ).toMinutes()
                )
                .sum();
    }

    public record DailyPlanSummary(
            int socialMinutes,
            int refreshMinutes,
            int myMinutes
    ) {
    }
}