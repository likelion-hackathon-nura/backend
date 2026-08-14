package org.example.nura.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.auth.dto.request.LoginRequest;
import org.example.nura.domain.auth.dto.request.SignupRequest;
import org.example.nura.domain.auth.dto.request.TokenRefreshRequest;
import org.example.nura.domain.auth.dto.response.AuthResponse;
import org.example.nura.domain.auth.dto.response.TokenRefreshResponse;
import org.example.nura.domain.auth.service.AuthService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "회원", description = "회원가입, 로그인, 온보딩 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원가입", description = "사용자가 회원가입을 합니다. 회원가입 성공 시, JWT 토큰이 발급됩니다.")
    public ApiResponse<AuthResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        AuthResponse response = authService.signup(request);

        return ApiResponse.success(
                "회원가입이 완료되었습니다.",
                response
        );
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자가 로그인합니다. 로그인 성공 시, JWT 토큰이 발급됩니다.")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ApiResponse.success(
                "로그인에 성공했습니다.",
                response
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 엑세스 토큰을 재발급합니다. 유효한 리프레시 토큰이 필요합니다.")
    public ApiResponse<TokenRefreshResponse> refresh(
            @Valid @RequestBody TokenRefreshRequest request
    ) {
        TokenRefreshResponse response =
                authService.refresh(request);

        return ApiResponse.success(
                "토큰이 재발급되었습니다.",
                response
        );
    }
}
