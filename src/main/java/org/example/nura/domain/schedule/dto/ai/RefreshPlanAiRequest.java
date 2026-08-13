package org.example.nura.domain.schedule.dto.ai;

import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.schedule.dto.context.PreviousCheckinContext;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.domain.user.entity.enums.SkinConcernType;
import org.example.nura.domain.user.entity.enums.SkinSensitivityLevel;
import org.example.nura.domain.user.entity.enums.SkinType;

import java.time.LocalDate;
import java.util.List;

public record RefreshPlanAiRequest(
        LocalDate date,

        ShiftType shiftType,

        int consecutiveWorkDays,
        int consecutiveNightShiftDays,

        Integer targetSleepMinutes,
        MealPattern mealPattern,

        List<RestActivityType> restActivities,

        SkinSensitivityLevel sensitivityLevel,
        SkinType skinType,
        List<SkinConcernType> skinConcerns,

        PreviousCheckinContext previousCheckin,

        Boolean previousRecoveryRoutineCompleted,

        List<AvailableSlotContext> availableSlots,
        List<AvailableSlotContext> skinRecoveryAvailableSlots
) {
}
