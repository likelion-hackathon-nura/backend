package org.example.nura.domain.schedule.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.nura.domain.schedule.entity.enums.ShiftType;

import java.time.LocalDate;

public record DutyScheduleItemRequest(

        @NotNull
        LocalDate date,

        @NotNull
        ShiftType shiftType
) {
}
