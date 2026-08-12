package org.example.nura.domain.schedule.dto.response;

import java.util.List;

public record DutyScheduleOcrResponse(
        List<DutyScheduleOcrItemResponse> schedules
) {
}
