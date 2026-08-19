package org.example.nura.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.MyInfoUpdateRequest;
import org.example.nura.domain.user.dto.response.MyInfoResponse;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyInfoService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MyInfoResponse getMyInfo(
            Long userId
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        return new MyInfoResponse(
                user.getNickname(),
                user.getEmail()
        );
    }

    @Transactional
    public void updateMyInfo(
            Long userId,
            MyInfoUpdateRequest request
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        validateUpdateRequest(request);

        if (request.nickname() != null
                && !request.nickname().isBlank()) {
            user.updateNickname(
                    request.nickname().trim()
            );
        }

        if (request.newPassword() != null
                && !request.newPassword().isBlank()) {
            user.updatePasswordHash(
                    passwordEncoder.encode(
                            request.newPassword()
                    )
            );
        }
    }

    private void validateUpdateRequest(
            MyInfoUpdateRequest request
    ) {
        boolean hasNickname =
                request.nickname() != null
                        && !request.nickname().isBlank();
        boolean hasPassword =
                request.newPassword() != null
                        && !request.newPassword().isBlank();
        boolean hasPasswordConfirm =
                request.newPasswordConfirm() != null
                        && !request.newPasswordConfirm().isBlank();

        if (!hasNickname && !hasPassword && !hasPasswordConfirm) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "변경할 정보를 하나 이상 입력해주세요."
            );
        }

        if (!hasPassword && hasPasswordConfirm) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "새 비밀번호를 입력해주세요."
            );
        }

        if (hasPassword && !hasPasswordConfirm) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호 확인을 입력해주세요."
            );
        }

        if (hasPassword
                && !request.newPassword().equals(request.newPasswordConfirm())) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "비밀번호 확인이 일치하지 않습니다."
            );
        }
    }
}
