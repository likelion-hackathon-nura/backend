package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.nura.domain.skin.entity.enums.CheckinSkinLevel;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CheckinResponse(

        @JsonProperty("checkin_id")
        Long checkinId,

        @JsonProperty("user_id")
        Long userId,

        LocalDate date,

        Integer fatigue,

        CheckinSkinLevel tightness,

        CheckinSkinLevel redness,

        @JsonProperty("recovery_level")
        RecoveryLevel recoveryLevel,

        @JsonProperty("analyzed_redness")
        SkinAnalysisLevel analyzedRedness,

        @JsonProperty("analyzed_moisture")
        SkinAnalysisLevel analyzedMoisture,

        @JsonProperty("analyzed_oiliness")
        SkinAnalysisLevel analyzedOiliness,

        @JsonProperty("analyzed_trouble")
        SkinAnalysisLevel analyzedTrouble,

        @JsonProperty("ai_comment")
        String aiComment,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}