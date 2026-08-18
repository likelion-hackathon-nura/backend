package org.example.nura.domain.skin.dto.response;

import org.example.nura.domain.cosmetics.entity.enums.CosmeticType;
import org.example.nura.domain.skin.entity.enums.SkinCareType;

import java.util.List;

public record SkinRoutineStepResponse(
        Integer stepOrder,
        SkinCareType careType,
        String careTypeKr,
        String careTypeEmoji,
        String title,
        String description,
        String recommendedIngredientDescription,
        List<String> recommendedIngredients,
        String categoryColor,
        Long cosmeticId,
        String cosmeticBrand,
        String cosmeticName,
        CosmeticType cosmeticType,
        String cosmeticUrl,
        String cosmeticCoreIngredients,
        List<String> precautions,
        String reason,
        List<String> productFeatures
) {
}