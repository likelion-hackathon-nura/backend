package org.example.nura.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomEventRecommendationItemResponse(
        LocalDate date,
        LocalDateTime startAt,
        LocalDateTime endAt,
        CustomEventRecommendationType type,
        String description,
        boolean bufferRelaxed
) {
}
