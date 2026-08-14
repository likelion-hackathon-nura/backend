package org.example.nura.domain.schedule.dto.plan;

import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.user.entity.enums.RestActivityType;

public record RefreshActivityAllocation(
        RestActivityType activityType,
        TimeInterval interval
) {
}
