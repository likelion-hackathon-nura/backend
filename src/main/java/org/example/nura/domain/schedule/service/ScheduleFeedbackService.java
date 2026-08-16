package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.request.ScheduleFeedbackRequest;
import org.example.nura.domain.schedule.dto.response.ScheduleFeedbackResponse;
import org.example.nura.domain.schedule.entity.ScheduleFeedback;
import org.example.nura.domain.schedule.entity.enums.FeedbackWeight;
import org.example.nura.domain.schedule.repository.ScheduleFeedbackRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleFeedbackService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ScheduleFeedbackRepository scheduleFeedbackRepository;

    public ScheduleFeedbackResponse getTodayFeedback(Long userId) {
        LocalDate today = LocalDate.now(KST);

        return scheduleFeedbackRepository
                .findByUserIdAndFeedbackDate(userId, today)
                .map(this::toResponse)
                .orElseGet(() -> new ScheduleFeedbackResponse(
                        today,
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public ScheduleFeedbackResponse submit(
            Long userId,
            ScheduleFeedbackRequest request
    ) {
        LocalDate today = LocalDate.now(KST);

        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new BaseException(ErrorCode.RESOURCE_NOT_FOUND)
                );

        if (scheduleFeedbackRepository.existsByUserIdAndFeedbackDate(
                userId,
                today
        )) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "오늘은 이미 피드백을 제출했습니다."
            );
        }

        Integer myDelta =
                toDelta(request.myWeight());

        Integer refreshDelta =
                toDelta(request.refreshWeight());

        user.applyScheduleFeedback(
                myDelta,
                refreshDelta
        );

        ScheduleFeedback feedback =
                ScheduleFeedback.create(
                        user,
                        today,
                        request.myWeight(),
                        request.refreshWeight(),
                        normalizeContents(request.feedbackContents())
                );

        try {
            return toResponse(
                    scheduleFeedbackRepository.save(feedback)
            );
        } catch (DataIntegrityViolationException e) {
            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "오늘은 이미 피드백을 제출했습니다."
            );
        }
    }

    private ScheduleFeedbackResponse toResponse(
            ScheduleFeedback feedback
    ) {
        return new ScheduleFeedbackResponse(
                feedback.getFeedbackDate(),
                feedback.getMyWeight(),
                feedback.getRefreshWeight(),
                feedback.getFeedbackContents()
        );
    }

    private Integer toDelta(FeedbackWeight weight) {
        if (weight == null) {
            return null;
        }

        return switch (weight) {
            case LOW -> 1;
            case NORMAL -> 0;
            case HIGH -> -1;
        };
    }

    private String normalizeContents(String contents) {
        if (contents == null || contents.isBlank()) {
            return null;
        }

        return contents.trim();
    }
}
