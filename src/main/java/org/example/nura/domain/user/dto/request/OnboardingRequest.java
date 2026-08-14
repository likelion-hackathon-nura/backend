package org.example.nura.domain.user.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.domain.user.entity.enums.SkinConcernType;
import org.example.nura.domain.user.entity.enums.SkinSensitivityLevel;
import org.example.nura.domain.user.entity.enums.SkinType;

import java.time.LocalTime;
import java.util.List;

public record OnboardingRequest(

        @NotNull
        LocalTime shiftDStart,

        @NotNull
        LocalTime shiftDEnd,

        @NotNull
        LocalTime shiftEStart,

        @NotNull
        LocalTime shiftEEnd,

        @NotNull
        LocalTime shiftNStart,

        @NotNull
        LocalTime shiftNEnd,

        @NotNull
        @Min(value = 240, message = "목표 수면 시간은 최소 4시간입니다.")
        @Max(value = 600, message = "목표 수면 시간은 최대 10시간입니다.")
        Integer targetSleepMinutes,

        @NotNull
        MealPattern mealPattern,

        @NotEmpty
        @Size(
                min = 1,
                max = 3,
                message = "휴식 활동은 1개 이상 3개 이하로 선택해주세요."
        )
        List<@NotNull RestActivityType> restActivities,

        @NotNull
        SkinSensitivityLevel sensitivityLevel,

        @NotNull
        SkinType skinType,

        @NotEmpty
        @Size(
                min = 1,
                max = 3,
                message = "피부 고민은 1개 이상 3개 이하로 선택해주세요."
        )
        List<@NotNull SkinConcernType> skinConcerns

) {
}
