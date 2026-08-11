package org.example.nura.domain.skin.dto.response;

import org.example.nura.domain.skin.entity.enums.CosmeticType;

public record CosmeticOcrResponse(
        String extractedBrand,
        String extractedName,
        CosmeticType extractedType,
        String extractedIngredient,
        String extractedCoreIngredient,
        String photoUrl
) {
}