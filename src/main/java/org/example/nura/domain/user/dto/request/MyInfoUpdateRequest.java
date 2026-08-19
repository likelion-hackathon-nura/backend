package org.example.nura.domain.user.dto.request;

import jakarta.validation.constraints.Size;

public record MyInfoUpdateRequest(
        @Size(
                max = 10,
                message = "닉네임은 10자 이하여야 합니다."
        )
        String nickname,

        @Size(
                min = 8,
                max = 20,
                message = "비밀번호는 8자 이상 20자 이하여야 합니다."
        )
        String newPassword,

        String newPasswordConfirm
) {
}
