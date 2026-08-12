package org.example.nura.domain.schedule.dto.response;

import java.util.List;

public record DutyScheduleWeeklyResponse(
        List<DutyScheduleDayResponse> schedules
) {
}
