package org.example.nura.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.schedule.entity.enums.FeedbackWeight;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

@Getter
@Entity
@Table(name = "schedule_feedback")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleFeedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_schedule_feedback_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "my_weight",
            nullable = false,
            length = 10
    )
    private FeedbackWeight myWeight;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "refresh_weight",
            nullable = false,
            length = 10
    )
    private FeedbackWeight refreshWeight;

    @Column(
            name = "feedback_contents",
            columnDefinition = "TEXT"
    )
    private String feedbackContents;

    private ScheduleFeedback(
            User user,
            FeedbackWeight myWeight,
            FeedbackWeight refreshWeight,
            String feedbackContents
    ) {
        this.user = user;
        this.myWeight = myWeight;
        this.refreshWeight = refreshWeight;
        this.feedbackContents = feedbackContents;
    }

    public static ScheduleFeedback create(
            User user,
            FeedbackWeight myWeight,
            FeedbackWeight refreshWeight,
            String feedbackContents
    ) {
        validateWeights(myWeight, refreshWeight);

        return new ScheduleFeedback(
                user,
                myWeight,
                refreshWeight,
                feedbackContents
        );
    }

    private static void validateWeights(
            FeedbackWeight myWeight,
            FeedbackWeight refreshWeight
    ) {
        if (myWeight == null || refreshWeight == null) {
            throw new IllegalArgumentException(
                    "마이타임과 리프레시타임 가중치는 필수입니다."
            );
        }
    }
}
