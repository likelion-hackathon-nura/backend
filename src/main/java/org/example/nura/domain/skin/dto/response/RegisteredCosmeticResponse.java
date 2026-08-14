package org.example.nura.domain.skin.dto.response;

import org.example.nura.domain.skin.entity.enums.CosmeticType;

import java.time.LocalDateTime;

public record RegisteredCosmeticResponse(
        Long registeredCosmeticId,
        Long userId,
        String cosmeticBrand,
        String cosmeticName,
        CosmeticType cosmeticType,
        LocalDateTime registeredAt
) {
}