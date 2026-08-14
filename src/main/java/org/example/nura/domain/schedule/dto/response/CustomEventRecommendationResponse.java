package org.example.nura.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomEventRecommendationResponse(
        int durationMinutes,
        List<CustomEventRecommendationItemResponse> recommendations
) {
}
