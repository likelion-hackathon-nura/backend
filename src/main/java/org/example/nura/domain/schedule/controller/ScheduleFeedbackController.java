package org.example.nura.domain.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.request.ScheduleFeedbackRequest;
import org.example.nura.domain.schedule.dto.response.ScheduleFeedbackResponse;
import org.example.nura.domain.schedule.service.ScheduleFeedbackService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지 피드백", description = "마이/리프레시 피드백 API")
@RestController
@RequestMapping("/api/schedule-feedback")
@RequiredArgsConstructor
public class ScheduleFeedbackController {

    private final ScheduleFeedbackService scheduleFeedbackService;

    @GetMapping("/today")
    @Operation(summary = "오늘 피드백 조회", description = "오늘 제출한 피드백이 있으면 반환하고, 없으면 빈 값을 반환합니다.")
    public ApiResponse<ScheduleFeedbackResponse> getToday(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "오늘 피드백 조회에 성공했습니다.",
                scheduleFeedbackService.getTodayFeedback(userId)
        );
    }

    @PostMapping
    @Operation(summary = "피드백 제출", description = "오늘의 마이/리프레시 피드백과 의견을 제출합니다.")
    public ApiResponse<ScheduleFeedbackResponse> submit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ScheduleFeedbackRequest request
    ) {
        return ApiResponse.success(
                "피드백이 저장되었습니다.",
                scheduleFeedbackService.submit(userId, request)
        );
    }
}
