package org.example.nura.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.MyInfoUpdateRequest;
import org.example.nura.domain.user.dto.response.MyInfoResponse;
import org.example.nura.domain.user.service.MyInfoService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "마이페이지", description = "마이페이지 내 정보 조회 및 수정")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MyInfoController {

    private final MyInfoService myInfoService;

    @Operation(summary = "내 정보 조회", description = "닉네임과 이메일을 조회합니다.")
    @GetMapping
    public ApiResponse<MyInfoResponse> getMyInfo(
            @AuthenticationPrincipal Long userId
    ) {
        MyInfoResponse response =
                myInfoService.getMyInfo(userId);

        return ApiResponse.success(
                "내 정보 조회에 성공했습니다.",
                response
        );
    }

    @Operation(summary = "내 정보 수정", description = "닉네임과 비밀번호를 수정합니다. 이메일은 수정할 수 없습니다.")
    @PatchMapping
    public ApiResponse<Void> updateMyInfo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MyInfoUpdateRequest request
    ) {
        myInfoService.updateMyInfo(
                userId,
                request
        );

        return ApiResponse.ok();
    }
}
