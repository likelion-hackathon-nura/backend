package org.example.nura.domain.schedule.dto.ai;

public record SkinRecoveryPlan(
        boolean enabled,
        Integer durationMinutes,
        String preferredSlotId
) {
}
