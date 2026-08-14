package org.example.nura.domain.skin.dto.command;

import org.example.nura.domain.skin.entity.enums.CosmeticType;
import org.example.nura.domain.skin.entity.enums.SkinCareType;

import java.util.List;

public record AiRoutineStepCommand(
        Integer stepOrder,
        SkinCareType careType,
        CosmeticType cosmeticType, // 매칭용 추천 제형 (SERUM, CREAM 등)
        String title,
        String description,
        String precautions,
        List<String> recommendedIngredients,
        String reason
) {}