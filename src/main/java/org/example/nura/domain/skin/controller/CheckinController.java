package org.example.nura.domain.skin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.CheckinCreateRequest;
import org.example.nura.domain.skin.dto.response.CheckinResponse;
import org.example.nura.domain.skin.dto.response.CheckinStatusResponse;
import org.example.nura.domain.skin.service.CheckinService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "체크인", description = "체크인 생성 API")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    @Operation(summary = "체크인 가능 여부 조회", description = "지정 날짜 기준으로 체크인 가능 여부를 조회합니다.")
    @GetMapping("/status")
    public ApiResponse<CheckinStatusResponse> status(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) LocalDate date
    ) {
        CheckinStatusResponse response = checkinService.getStatus(
                userId,
                date
        );

        return ApiResponse.success(
                "체크인 가능 여부 조회에 성공했습니다.",
                response
        );
    }

    @Operation(summary = "체크인 생성", description = "사용자의 당일 체크인을 생성하고 SkinRoutine 생성을 시작합니다.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CheckinResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute CheckinCreateRequest request
    ) {
        CheckinResponse response = checkinService.create(userId, request);

        return ApiResponse.success(
                "체크인이 완료되었습니다.",
                response
        );
    }
}