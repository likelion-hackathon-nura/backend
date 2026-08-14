package org.example.nura.domain.schedule.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomEventCheckResponse(
        CustomEventCheckStatus status,
        Integer refreshDecreaseMinutes,
        Integer currentRefreshMinutes,
        Integer expectedRefreshMinutes,
        Integer minimumRecommendedRefreshMinutes,
        Boolean belowMinimumRecommended
) {
}
