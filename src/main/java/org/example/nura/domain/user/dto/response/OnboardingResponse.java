package org.example.nura.domain.user.dto.response;

public record OnboardingResponse(
        Long userId,
        boolean onboardingCompleted
) {
}
