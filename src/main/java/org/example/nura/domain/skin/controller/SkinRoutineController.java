package org.example.nura.domain.skin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.response.SkinRoutineResponse;
import org.example.nura.domain.skin.service.SkinRoutineService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "3분 회복 루틴", description = "체크인 기반 루틴 생성/조회 API")
@RestController
@RequestMapping("/api/skin-routines")
@RequiredArgsConstructor
public class SkinRoutineController {

    private final SkinRoutineService skinRoutineService;

    @Operation(summary = "맞춤 회복 루틴 생성", description = "오늘 체크인과 피부 분석 결과를 기반으로 루틴 단계를 생성합니다.")
    @PostMapping("/generate")
    public ApiResponse<SkinRoutineResponse> generate(
            @AuthenticationPrincipal Long userId
    ) {
        SkinRoutineResponse response =
                skinRoutineService.generateTodayRoutine(userId);

        return ApiResponse.success(
                "맞춤 회복 루틴 생성에 성공했습니다.",
                response
        );
    }

    @Operation(summary = "오늘의 회복 루틴 조회", description = "오늘 날짜 기준 회복 루틴과 단계를 조회합니다.")
    @GetMapping("/today")
    public ApiResponse<SkinRoutineResponse> today(
            @AuthenticationPrincipal Long userId
    ) {
        SkinRoutineResponse response =
                skinRoutineService.getTodayRoutine(userId);

        return ApiResponse.success(
                "오늘의 회복 루틴 조회에 성공했습니다.",
                response
        );
    }

    @Operation(summary = "오늘의 회복 루틴 완료", description = "오늘 회복 루틴을 완료 상태로 변경합니다.")
    @PatchMapping("/today/complete")
    public ApiResponse<SkinRoutineResponse> completeToday(
            @AuthenticationPrincipal Long userId
    ) {
        SkinRoutineResponse response =
                skinRoutineService.completeTodayRoutine(userId);

        return ApiResponse.success(
                "오늘의 회복 루틴이 완료 처리되었습니다.",
                response
        );
    }
}

