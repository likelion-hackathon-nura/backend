package org.example.nura.domain.schedule.dto.plan;

import org.example.nura.domain.schedule.entity.CustomEvent;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;

import java.time.LocalDateTime;

public record PlannedTimeBlock(
        TimeCategory category,
        String label,
        LocalDateTime startAt,
        LocalDateTime endAt,
        TimeBlockSource source,
        CustomEvent customEvent
) {
}
