package org.example.nura.domain.skin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SkinRoutineFeedbackRequest(
        @NotBlank(message = "3분 회복 모드 피드백을 입력해주세요.")
        @Size(max = 500, message = "피드백은 500자 이하로 입력해주세요.")
        String contents
) {}