package org.example.nura.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;

import java.time.LocalDateTime;

public record CustomEventCheckRequest(

        @NotNull
        TimeCategory category,

        @NotBlank
        @Size(max = 50)
        String eventName,

        @NotNull
        LocalDateTime startAt,

        @NotNull
        LocalDateTime endAt
) {
}
