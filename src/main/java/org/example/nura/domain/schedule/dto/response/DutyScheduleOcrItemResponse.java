package org.example.nura.domain.schedule.dto.response;

import org.example.nura.domain.schedule.entity.enums.ShiftType;

import java.time.DayOfWeek;
import java.time.LocalDate;

public record DutyScheduleOcrItemResponse(
        LocalDate date,
        DayOfWeek dayOfWeek,
        ShiftType shiftType,
        boolean editable
) {
}
