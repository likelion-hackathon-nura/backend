package org.example.nura.domain.schedule.dto.plan;

import org.example.nura.domain.schedule.dto.context.TimeInterval;

import java.util.List;

public record DailyPlanGenerationResult(
        List<TimeInterval> workIntervals,
        List<TimeInterval> sleepIntervals,
        List<TimeInterval> mealIntervals,
        RefreshAllocationResult refreshAllocation,
        List<TimeInterval> myIntervals
) {
}
