package org.example.nura.domain.home.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.home.dto.response.HomeResponse;
import org.example.nura.domain.home.service.HomeService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(
        name = "홈",
        description = "오늘 하루 시간 설계 및 홈 조회 API"
)
public class HomeController {

    private final HomeService homeService;

    @Operation(
            summary = "오늘 홈 최초 생성",
            description = "오늘의 시간 설계가 없으면 생성하고, 이미 존재하면 기존 결과를 반환합니다."
    )
    @PostMapping("/today")
    public ApiResponse<HomeResponse> ensureToday(
            @AuthenticationPrincipal Long userId
    ) {
        HomeResponse response =
                homeService.ensureToday(userId);

        if (response.socialMinutes() == null) {
            return ApiResponse.success(
                    "오늘 등록된 근무표가 없습니다.",
                    response
            );
        }

        return ApiResponse.success(
                "오늘의 시간 설계가 생성되었습니다.",
                response
        );
    }

    @Operation(
            summary = "오늘 홈 조회",
            description = "이미 생성된 오늘의 시간 설계 결과를 조회합니다."
    )
    @GetMapping("/today")
    public ApiResponse<HomeResponse> getToday(
            @AuthenticationPrincipal Long userId
    ) {
        HomeResponse response =
                homeService.getToday(userId);

        if (response.socialMinutes() == null) {
            return ApiResponse.success(
                    "오늘 등록된 근무표가 없습니다.",
                    response
            );
        }

        return ApiResponse.success(
                "오늘의 시간 설계 조회에 성공했습니다.",
                response
        );
    }
}