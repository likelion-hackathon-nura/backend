package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.nura.domain.skin.entity.enums.CosmeticType;
import org.example.nura.domain.skin.entity.enums.SkinCareType;

public record SkinRoutineStepResponse(
        @JsonProperty("step_order")
        Integer stepOrder,
        @JsonProperty("care_type")
        SkinCareType careType,
        String title,
        String description,
        String precautions,
        @JsonProperty("recommended_ingredients")
        String recommendedIngredients,
        String reason,
        @JsonProperty("cosmetic_id")
        Long cosmeticId,
        @JsonProperty("cosmetic_brand")
        String cosmeticBrand,
        @JsonProperty("cosmetic_name")
        String cosmeticName,
        @JsonProperty("cosmetic_type")
        CosmeticType cosmeticType,
        @JsonProperty("cosmetic_url")
        String cosmeticUrl
) {
}

