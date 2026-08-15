package org.example.nura.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomEventCreateResponse(
        Long customEventId,
        TimeCategory category,
        String eventName,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
