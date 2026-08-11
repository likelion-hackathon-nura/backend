package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record CheckinStatusResponse(
        LocalDate date,
        @JsonProperty("can_checkin")
        boolean canCheckin,
        String reason,
        @JsonProperty("existing_checkin_id")
        Long existingCheckinId
) {
}

