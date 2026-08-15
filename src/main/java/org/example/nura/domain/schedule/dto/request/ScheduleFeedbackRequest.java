package org.example.nura.domain.schedule.dto.request;

import org.example.nura.domain.schedule.entity.enums.FeedbackWeight;

public record ScheduleFeedbackRequest(
        FeedbackWeight myWeight,
        FeedbackWeight refreshWeight,
        String feedbackContents
) {
}
