package org.example.nura.domain.skin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.SkinRoutineFeedbackRequest;
import org.example.nura.domain.skin.dto.response.SkinRoutineFeedbackResponse;
import org.example.nura.domain.skin.service.SkinRoutineFeedbackService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3분 회복 모드 피드백", description = "3분 회복 모드 루틴 피드백 제출 API")
@RestController
@RequestMapping("/api/skin-feedback")
@RequiredArgsConstructor
public class SkinRoutineFeedbackController {

    private final SkinRoutineFeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "3분 회복 모드 피드백 제출", description = "3분 회복 모드 사용 후 남긴 피드백을 저장합니다.")
    public ApiResponse<SkinRoutineFeedbackResponse> submit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SkinRoutineFeedbackRequest request
    ) {
        return ApiResponse.success("피드백이 성공적으로 등록되었습니다.", feedbackService.submitFeedback(userId, request));
    }
}