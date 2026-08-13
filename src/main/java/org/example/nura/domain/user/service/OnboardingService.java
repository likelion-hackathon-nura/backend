package org.example.nura.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.OnboardingRequest;
import org.example.nura.domain.user.dto.response.OnboardingResponse;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.entity.UserRestActivity;
import org.example.nura.domain.user.entity.UserSkin;
import org.example.nura.domain.user.entity.UserSkinConcern;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.domain.user.repository.UserRestActivityRepository;
import org.example.nura.domain.user.repository.UserSkinConcernRepository;
import org.example.nura.domain.user.repository.UserSkinRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserRestActivityRepository userRestActivityRepository;
    private final UserSkinRepository userSkinRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;

    @Transactional
    public OnboardingResponse complete(
            Long userId,
            OnboardingRequest request
    ) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        if (user.isOnboardingCompleted()) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "이미 온보딩을 완료한 사용자입니다."
            );
        }

        validateRequest(request);

        user.completeOnboarding(
                request.shiftDStart(),
                request.shiftDEnd(),
                request.shiftEStart(),
                request.shiftEEnd(),
                request.shiftNStart(),
                request.shiftNEnd(),
                request.targetSleepMinutes(),
                request.mealPattern()
        );

        List<UserRestActivity> restActivities =
                request.restActivities()
                        .stream()
                        .map(activity ->
                                UserRestActivity.create(
                                        user,
                                        activity
                                )
                        )
                        .toList();

        userRestActivityRepository.saveAll(restActivities);

        UserSkin userSkin = UserSkin.create(
                user,
                request.sensitivityLevel(),
                request.skinType()
        );

        userSkinRepository.save(userSkin);

        List<UserSkinConcern> skinConcerns =
                request.skinConcerns()
                        .stream()
                        .map(concern ->
                                UserSkinConcern.create(
                                        userSkin,
                                        concern
                                )
                        )
                        .toList();

        userSkinConcernRepository.saveAll(skinConcerns);

        return new OnboardingResponse(
                user.getId(),
                user.isOnboardingCompleted()
        );
    }

    private void validateRequest(OnboardingRequest request) {

        if (request.restActivities()
                .stream()
                .distinct()
                .count() != request.restActivities().size()) {

            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "휴식 활동은 중복 선택할 수 없습니다."
            );
        }

        if (request.skinConcerns()
                .stream()
                .distinct()
                .count() != request.skinConcerns().size()) {

            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "피부 고민은 중복 선택할 수 없습니다."
            );
        }
    }
}
