package org.example.nura.domain.skin.dto.response;

import java.time.LocalDateTime;

public record SkinRoutineFeedbackResponse(
        Long feedbackId,
        String contents,
        LocalDateTime createdAt
) {}