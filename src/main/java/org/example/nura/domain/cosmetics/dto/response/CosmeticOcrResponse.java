package org.example.nura.domain.cosmetics.dto.response;

public record CosmeticOcrResponse(
        String cosmeticIngredients,
        String coreIngredients
) {
}