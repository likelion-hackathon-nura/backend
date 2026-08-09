package org.example.nura.domain.auth.dto.response;

public record AuthResponse(
        Long userId,
        String nickname,
        String accessToken,
        String refreshToken,
        boolean onboardingCompleted
) {
}
