package org.example.nura.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.auth.dto.request.LoginRequest;
import org.example.nura.domain.auth.dto.request.SignupRequest;
import org.example.nura.domain.auth.dto.request.TokenRefreshRequest;
import org.example.nura.domain.auth.dto.response.AuthResponse;
import org.example.nura.domain.auth.dto.response.TokenRefreshResponse;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.security.JwtTokenProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse signup(SignupRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BaseException(
                    ErrorCode.EMAIL_ALREADY_EXISTS
            );
        }

        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = User.create(
                request.email(),
                passwordHash,
                request.nickname()
        );

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(savedUser.getId());

        String refreshToken =
                jwtTokenProvider.createRefreshToken(savedUser.getId());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getNickname(),
                accessToken,
                refreshToken,
                savedUser.isOnboardingCompleted()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.INVALID_LOGIN_CREDENTIALS
                        )
                );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        )) {
            throw new BaseException(
                    ErrorCode.INVALID_LOGIN_CREDENTIALS
            );
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId());

        String refreshToken =
                jwtTokenProvider.createRefreshToken(user.getId());

        return new AuthResponse(
                user.getId(),
                user.getNickname(),
                accessToken,
                refreshToken,
                user.isOnboardingCompleted()
        );
    }

    @Transactional
    public TokenRefreshResponse refresh(
            TokenRefreshRequest request
    ) {

        Long userId =
                jwtTokenProvider.getUserIdFromRefreshToken(
                        request.refreshToken()
                );

        if (!userRepository.existsById(userId)) {
            throw new BaseException(
                    ErrorCode.INVALID_TOKEN
            );
        }

        String newAccessToken =
                jwtTokenProvider.createAccessToken(userId);

        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(userId);

        return new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken
        );
    }
}
