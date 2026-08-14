package org.example.nura.domain.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.request.CustomEventCheckRequest;
import org.example.nura.domain.schedule.dto.response.CustomEventCreateResponse;
import org.example.nura.domain.schedule.dto.response.CustomEventCheckResponse;
import org.example.nura.domain.schedule.dto.response.CustomEventRecommendationResponse;
import org.example.nura.domain.schedule.service.CustomEventService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "일정 관리", description = "사용자 일정 추가 플로우 API")
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class CustomEventController {

    private final CustomEventService customEventService;

    @Operation(
            summary = "일정 추가 가능 여부 확인",
            description = "오늘 홈 기준으로 새 일정이 회복 시간을 줄이는지 확인합니다."
    )
    @PostMapping("/check")
    public ApiResponse<CustomEventCheckResponse> check(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CustomEventCheckRequest request
    ) {
        CustomEventCheckResponse response =
                customEventService.check(
                        userId,
                        request
                );

        String message =
                response.status() == org.example.nura.domain.schedule.dto.response.CustomEventCheckStatus.AVAILABLE
                        ? "바로 등록 가능한 일정입니다."
                        : "일정 등록 시 회복 시간이 감소합니다.";

        return ApiResponse.success(
                message,
                response
        );
    }

    @Operation(
            summary = "일정 등록",
            description = "일정을 저장하고, 오늘인 경우 홈 TimeBlock을 즉시 갱신합니다."
    )
    @PostMapping
    public ApiResponse<CustomEventCreateResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CustomEventCheckRequest request
    ) {
        CustomEventCreateResponse response =
                customEventService.create(
                        userId,
                        request
                );

        return ApiResponse.success(
                "일정이 등록되었습니다.",
                response
        );
    }

    @Operation(
            summary = "추천 일정 조회",
            description = "일정과 같은 길이의 추천 가능한 시간대를 최대 3개까지 반환합니다."
    )
    @PostMapping("/recommendations")
    public ApiResponse<CustomEventRecommendationResponse> recommendations(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CustomEventCheckRequest request
    ) {
        CustomEventRecommendationResponse response =
                customEventService.recommendations(
                        userId,
                        request
                );

        return ApiResponse.success(
                "추천 일정을 조회했습니다.",
                response
        );
    }
}
