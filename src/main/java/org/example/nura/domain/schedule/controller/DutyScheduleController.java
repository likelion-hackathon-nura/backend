package org.example.nura.domain.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.response.DutyScheduleWeeklyResponse;
import org.example.nura.domain.schedule.service.DutyScheduleService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "근무표", description = "근무표 조회 및 등록/수정 API")
@RestController
@RequestMapping("api/schedules")
@RequiredArgsConstructor
public class DutyScheduleController {

    private final DutyScheduleService dutyScheduleService;

    @Operation(
            summary = "근무표 기간 조회",
            description = "지정한 시작일과 종료일 사이의 근무표를 조회합니다."
    )
    @GetMapping
    public ApiResponse<DutyScheduleWeeklyResponse> getSchedules(
            @AuthenticationPrincipal Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        DutyScheduleWeeklyResponse response =
                dutyScheduleService.getSchedules(
                        userId,
                        startDate,
                        endDate
                );

        return ApiResponse.success(
                "근무표 조회에 성공했습니다.",
                response
        );
    }
}
