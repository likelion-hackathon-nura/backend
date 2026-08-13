package org.example.nura.domain.schedule.dto.context;

import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.domain.user.entity.enums.SkinConcernType;
import org.example.nura.domain.user.entity.enums.SkinSensitivityLevel;
import org.example.nura.domain.user.entity.enums.SkinType;

import java.time.LocalDate;
import java.util.List;

public record DailyPlanContext(
        LocalDate date,

        ShiftType todayShiftType,
        ShiftType previousShiftType,

        int consecutiveWorkDays,
        int consecutiveNightShiftDays,

        Integer targetSleepMinutes,
        MealPattern mealPattern,
        List<RestActivityType> restActivities,

        SkinSensitivityLevel sensitivityLevel,
        SkinType skinType,
        List<SkinConcernType> skinConcerns,

        PreviousCheckinContext previousCheckin,
        Boolean previousRecoveryRoutineCompleted
) {
}
