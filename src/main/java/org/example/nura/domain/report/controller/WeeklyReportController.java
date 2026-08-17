package org.example.nura.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.report.dto.response.WeeklyReportResponse;
import org.example.nura.domain.report.service.WeeklyReportService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "리포트 관리", description = "주간 웰니스 및 피부 분석 리포트 API")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(
            summary = "주간 리포트 조회",
            description = "지정한 날짜가 속한 주의 AI 코멘트, 회복 추세 그래프, 3-Time 밸런스 비율을 조회합니다."
    )
    @GetMapping("/weekly")
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        WeeklyReportResponse response = weeklyReportService.getWeeklyReport(userId, targetDate);
        return ApiResponse.success("주간 리포트 조회에 성공했습니다.", response);
    }
}