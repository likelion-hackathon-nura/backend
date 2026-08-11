package org.example.nura.domain.skin.dto.request;

import org.example.nura.domain.skin.entity.enums.CosmeticType;

public record RegisteredCosmeticCreateRequest(
        String cosmeticBrand,
        String cosmeticName,
        CosmeticType cosmeticType,
        String cosmeticIngredient,
        String cosmeticCoreIngredient,
        String cosmeticUrl
) {
}