package org.example.nura.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.auth.dto.request.LoginRequest;
import org.example.nura.domain.auth.dto.request.SignupRequest;
import org.example.nura.domain.auth.dto.request.TokenRefreshRequest;
import org.example.nura.domain.auth.dto.response.AuthResponse;
import org.example.nura.domain.auth.dto.response.TokenRefreshResponse;
import org.example.nura.domain.report.repository.WeeklyReportRepository;
import org.example.nura.domain.schedule.repository.CustomEventRepository;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.schedule.repository.ScheduleFeedbackRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.skin.repository.CheckinRepository;
import org.example.nura.domain.skin.repository.RegisteredCosmeticRepository;
import org.example.nura.domain.skin.repository.RoutineStepRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.entity.UserSkin;
import org.example.nura.domain.user.repository.UserRestActivityRepository;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.domain.user.repository.UserSkinConcernRepository;
import org.example.nura.domain.user.repository.UserSkinRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.security.JwtTokenProvider;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final ScheduleFeedbackRepository scheduleFeedbackRepository;
    private final CustomEventRepository customEventRepository;
    private final DutyScheduleRepository dutyScheduleRepository;
    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final RegisteredCosmeticRepository registeredCosmeticRepository;
    private final RoutineStepRepository routineStepRepository;
    private final SkinRoutineRepository skinRoutineRepository;
    private final CheckinRepository checkinRepository;
    private final UserRestActivityRepository userRestActivityRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;
    private final UserSkinRepository userSkinRepository;
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
            Throwable cause = e.getCause();
            if (cause instanceof ConstraintViolationException cv
                    && "uk_users_email".equals(cv.getConstraintName())) {
                throw new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw e;
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

    public void logout() {
        // Stateless JWT: the client discards its tokens.
    }

    @Transactional
    public void deleteAccount(Long userId) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        UserSkin userSkin = userSkinRepository.findByUserId(userId)
                .orElse(null);

        weeklyReportRepository.deleteAllByUserId(userId);
        scheduleFeedbackRepository.deleteAllByUserId(userId);
        userRestActivityRepository.deleteAllByUserId(userId);
        routineStepRepository.deleteAllByRoutineCheckinUserId(userId);
        skinRoutineRepository.deleteAllByCheckinUserId(userId);
        checkinRepository.deleteAllByUserId(userId);
        registeredCosmeticRepository.deleteAllByUserId(userId);
        customEventRepository.deleteAllByUserId(userId);
        dutyScheduleRepository.deleteAllByUserId(userId);

        for (DailyTimeAllocation allocation :
                dailyTimeAllocationRepository.findAllByUserId(userId)) {
            timeBlockRepository.deleteAllByAllocationId(allocation.getId());
        }

        dailyTimeAllocationRepository.deleteAllByUserId(userId);

        if (userSkin != null) {
            userSkinConcernRepository.deleteAllByUserSkinId(
                    userSkin.getId()
            );
            userSkinRepository.deleteByUserId(userId);
        }

        userRepository.delete(user);
    }
}
