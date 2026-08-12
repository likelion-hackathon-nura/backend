package org.example.nura.domain.user.dto.response;

import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.domain.user.entity.enums.SkinConcernType;
import org.example.nura.domain.user.entity.enums.SkinSensitivityLevel;
import org.example.nura.domain.user.entity.enums.SkinType;

import java.util.List;

public record UserPreferencesResponse(
        Integer targetSleepMinutes,
        MealPattern mealPattern,
        List<RestActivityType> restActivities,
        SkinSensitivityLevel sensitivityLevel,
        SkinType skinType,
        List<SkinConcernType> skinConcerns
) {
}
