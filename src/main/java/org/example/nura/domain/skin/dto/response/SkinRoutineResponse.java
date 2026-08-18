package org.example.nura.domain.skin.dto.response;

import org.example.nura.domain.skin.entity.enums.RecoveryLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SkinRoutineResponse(
        Long routineId,
        Long checkinId,
        LocalDate date,
        RecoveryLevel recoveryLevel,
        Integer totalStepCount,
        String summaryComment,
        boolean completed,
        List<SkinRoutineStepResponse> steps,
        LocalDateTime createdAt
) {
}