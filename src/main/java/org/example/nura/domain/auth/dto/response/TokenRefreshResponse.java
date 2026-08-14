package org.example.nura.domain.auth.dto.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
