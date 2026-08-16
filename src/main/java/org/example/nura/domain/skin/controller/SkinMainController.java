package org.example.nura.domain.skin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.response.SkinMainTodayResponse;
import org.example.nura.domain.skin.service.SkinMainService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "피부 메인", description = "피부 탭 메인 상태 조회 API")
@RestController
@RequestMapping("/api/skin")
@RequiredArgsConstructor
public class SkinMainController {

    private final SkinMainService skinMainService;

    @Operation(summary = "오늘의 피부 탭 메인 상태 조회", description = "피부 메인 화면 진입 시 필요한 종합 정보를 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<SkinMainTodayResponse> getTodayMain(
            @AuthenticationPrincipal Long userId
    ) {
        SkinMainTodayResponse response = skinMainService.getTodayMain(userId, LocalDate.now());
        return ApiResponse.success("오늘의 피부 메인 상태 조회에 성공했습니다.", response);
    }
}