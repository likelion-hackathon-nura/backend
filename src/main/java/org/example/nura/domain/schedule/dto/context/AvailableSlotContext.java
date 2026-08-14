package org.example.nura.domain.schedule.dto.context;

import java.time.LocalDateTime;

public record AvailableSlotContext(
        String slotId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        long durationMinutes
) {
}
