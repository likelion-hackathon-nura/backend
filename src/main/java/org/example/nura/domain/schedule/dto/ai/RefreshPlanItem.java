package org.example.nura.domain.schedule.dto.ai;

import org.example.nura.domain.user.entity.enums.RestActivityType;

public record RefreshPlanItem(
        RestActivityType activityType,
        int durationMinutes,
        String preferredSlotId
) {
}
