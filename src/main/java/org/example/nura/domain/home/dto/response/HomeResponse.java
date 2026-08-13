package org.example.nura.domain.home.dto.response;

import org.example.nura.domain.schedule.entity.enums.ShiftType;

import java.time.LocalDate;
import java.util.List;

public record HomeResponse(
        LocalDate date,
        String nickname,
        ShiftType shiftType,

        int socialMinutes,
        int refreshMinutes,
        int myMinutes,

        String aiComment,

        List<HomeBadgeResponse> badges,
        List<HomeBlockResponse> blocks
) {
}
