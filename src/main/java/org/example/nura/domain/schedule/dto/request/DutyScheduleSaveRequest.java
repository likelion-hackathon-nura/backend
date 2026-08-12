package org.example.nura.domain.schedule.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.example.nura.domain.schedule.entity.enums.DutyScheduleSource;

import java.util.List;

public record DutyScheduleSaveRequest(

        @NotNull
        DutyScheduleSource source,

        @NotEmpty
        List<@Valid DutyScheduleItemRequest> schedules
) {
}
