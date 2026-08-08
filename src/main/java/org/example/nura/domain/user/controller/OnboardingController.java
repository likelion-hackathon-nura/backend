package org.example.nura.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.OnboardingRequest;
import org.example.nura.domain.user.dto.response.OnboardingResponse;
import org.example.nura.domain.user.service.OnboardingService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원", description = "회원가입, 로그인, 온보딩 API")
@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(summary = "온보딩 완료", description = "사용자가 온보딩을 완료했음을 서버에 알립니다. 온보딩 완료 시, 사용자의 기본 정보가 저장됩니다.")
    @PostMapping
    public ApiResponse<OnboardingResponse> complete(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponse response =
                onboardingService.complete(userId, request);

        return ApiResponse.success(
                "온보딩이 완료되었습니다.",
                response
        );
    }
}
