package org.example.nura.domain.schedule.dto.context;

import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;

public record PreviousCheckinContext(
        Integer fatigue,
        Integer tightness,
        Integer redness,

        SkinAnalysisLevel analyzedRedness,
        SkinAnalysisLevel analyzedMoisture,
        SkinAnalysisLevel analyzedOiliness,
        SkinAnalysisLevel analyzedTrouble
) {
}
