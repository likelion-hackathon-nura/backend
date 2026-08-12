package org.example.nura.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.user.dto.request.UserPreferencesUpdateRequest;
import org.example.nura.domain.user.dto.response.UserPreferencesResponse;
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
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserRestActivityRepository userRestActivityRepository;
    private final UserSkinRepository userSkinRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;

    public UserPreferencesResponse getPreferences(Long userId) {

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        UserSkin userSkin = userSkinRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        List<UserRestActivity> restActivities =
                userRestActivityRepository.findAllByUserId(userId);

        List<UserSkinConcern> skinConcerns =
                userSkinConcernRepository.findAllByUserSkinId(
                        userSkin.getId()
                );

        return new UserPreferencesResponse(
                user.getTargetSleepMinutes(),
                user.getMealPattern(),

                restActivities.stream()
                        .map(UserRestActivity::getActivityType)
                        .toList(),

                userSkin.getSensitivityLevel(),
                userSkin.getSkinType(),

                skinConcerns.stream()
                        .map(UserSkinConcern::getConcernType)
                        .toList()
        );
    }

    @Transactional
    public void updatePreferences(
            Long userId,
            UserPreferencesUpdateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        UserSkin userSkin = userSkinRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        validateRequest(request);

        user.updatePreferences(
                request.targetSleepMinutes(),
                request.mealPattern()
        );

        userSkin.update(
                request.sensitivityLevel(),
                request.skinType()
        );

        userRestActivityRepository.deleteAllByUserId(userId);
        userRestActivityRepository.flush();

        List<UserRestActivity> restActivities =
                request.restActivities()
                        .stream()
                        .map(activity ->
                                UserRestActivity.create(user, activity)
                        )
                        .toList();

        userRestActivityRepository.saveAll(restActivities);

        userSkinConcernRepository.deleteAllByUserSkinId(userSkin.getId());
        userSkinConcernRepository.flush();

        List<UserSkinConcern> skinConcerns =
                request.skinConcerns()
                        .stream()
                        .map(concern ->
                                UserSkinConcern.create(userSkin, concern)
                        )
                        .toList();

        userSkinConcernRepository.saveAll(skinConcerns);
    }

    private void validateRequest(UserPreferencesUpdateRequest request) {

        if (request.restActivities().stream().distinct().count()
                != request.restActivities().size()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "휴식 활동은 중복 선택할 수 없습니다."
            );
        }

        if (request.skinConcerns().stream().distinct().count()
                != request.skinConcerns().size()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "피부 고민은 중복 선택할 수 없습니다."
            );
        }
    }
}
