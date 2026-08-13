package org.example.nura.domain.schedule.dto.context;

import java.time.LocalDateTime;

public record TimeInterval(
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
