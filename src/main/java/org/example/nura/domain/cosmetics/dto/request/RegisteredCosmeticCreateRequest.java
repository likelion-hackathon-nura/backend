package org.example.nura.domain.cosmetics.dto.request;

import org.example.nura.domain.cosmetics.entity.enums.CosmeticType;

public record RegisteredCosmeticCreateRequest(
        String cosmeticBrand,
        String cosmeticName,
        CosmeticType cosmeticType,
        String cosmeticIngredients,
        String coreIngredients,
        String cosmeticUrl
) {
}