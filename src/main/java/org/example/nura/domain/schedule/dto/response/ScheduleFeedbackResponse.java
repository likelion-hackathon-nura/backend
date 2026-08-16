package org.example.nura.domain.schedule.dto.response;

import org.example.nura.domain.schedule.entity.enums.FeedbackWeight;

import java.time.LocalDate;

public record ScheduleFeedbackResponse(
        LocalDate feedbackDate,
        FeedbackWeight myWeight,
        FeedbackWeight refreshWeight,
        String feedbackContents
) {
}
