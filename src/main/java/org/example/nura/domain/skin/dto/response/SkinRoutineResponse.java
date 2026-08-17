package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.nura.domain.skin.entity.enums.RecoveryLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SkinRoutineResponse(
        @JsonProperty("routine_id")
        Long routineId,

        @JsonProperty("checkin_id")
        Long checkinId,

        LocalDate date,

        @JsonProperty("recovery_level")
        RecoveryLevel recoveryLevel,

        @JsonProperty("total_step_count")
        Integer totalStepCount,

        @JsonProperty("summary_comment")
        String summaryComment,

        boolean completed,

        List<SkinRoutineStepResponse> steps,

        @JsonProperty("created_at")
        LocalDateTime createdAt
) {
}