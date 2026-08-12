package org.example.nura.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.UserPreferencesUpdateRequest;
import org.example.nura.domain.user.dto.response.UserPreferencesResponse;
import org.example.nura.domain.user.service.UserPreferenceService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원", description = "회원가입, 로그인, 온보딩 API")
@RestController
@RequestMapping("/api/me/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping
    @Operation(summary = "온보딩 조회", description = "사용자의 온보딩 정보를 조회합니다.")
    public ApiResponse<UserPreferencesResponse> getPreferences(
            @AuthenticationPrincipal Long userId
    ) {
        return ApiResponse.success(
                "온보딩 조회에 성공했습니다.",
                userPreferenceService.getPreferences(userId)
        );
    }

    @PutMapping
    @Operation(summary = "온보딩 수정", description = "사용자의 온보딩 정보를 수정합니다.")
    public ApiResponse<Void> updatePreferences(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserPreferencesUpdateRequest request
    ) {
        userPreferenceService.updatePreferences(
                userId,
                request
        );

        return ApiResponse.ok();
    }
}
