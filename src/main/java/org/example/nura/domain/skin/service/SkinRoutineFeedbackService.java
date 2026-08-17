package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.request.SkinRoutineFeedbackRequest;
import org.example.nura.domain.skin.dto.response.SkinRoutineFeedbackResponse;
import org.example.nura.domain.skin.entity.SkinRoutineFeedback;
import org.example.nura.domain.skin.repository.SkinRoutineFeedbackRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkinRoutineFeedbackService {

    private final UserRepository userRepository;
    private final SkinRoutineFeedbackRepository feedbackRepository;

    @Transactional
    public SkinRoutineFeedbackResponse submitFeedback(Long userId, SkinRoutineFeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(ErrorCode.RESOURCE_NOT_FOUND));

        SkinRoutineFeedback feedback = SkinRoutineFeedback.create(
                user,
                request.contents().trim()
        );

        SkinRoutineFeedback saved = feedbackRepository.save(feedback);
        return new SkinRoutineFeedbackResponse(
                saved.getId(),
                saved.getContents(),
                saved.getCreatedAt()
        );
    }
}