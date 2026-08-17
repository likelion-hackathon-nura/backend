package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.nura.domain.cosmetics.entity.enums.CosmeticType;
import org.example.nura.domain.skin.entity.enums.SkinCareType;

import java.util.List;

public record SkinRoutineStepResponse(
        @JsonProperty("step_order") Integer stepOrder,
        @JsonProperty("care_type") SkinCareType careType,
        @JsonProperty("care_type_kr") String careTypeKr,
        @JsonProperty("care_type_emoji") String careTypeEmoji,

        String title,
        String description,

        @JsonProperty("recommended_ingredient_description") String recommendedIngredientDescription,

        @JsonProperty("recommended_ingredients") List<String> recommendedIngredients,

        @JsonProperty("category_color") String categoryColor,


        @JsonProperty("cosmetic_id") Long cosmeticId,
        @JsonProperty("cosmetic_brand") String cosmeticBrand,
        @JsonProperty("cosmetic_name") String cosmeticName,
        @JsonProperty("cosmetic_type") CosmeticType cosmeticType,
        @JsonProperty("cosmetic_url") String cosmeticUrl,
        @JsonProperty("cosmetic_core_ingredients") String cosmeticCoreIngredients,

        List<String> precautions,
        String reason,

        @JsonProperty("product_features") List<String> productFeatures
) {}