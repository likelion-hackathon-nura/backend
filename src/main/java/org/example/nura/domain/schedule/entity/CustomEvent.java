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
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "custom_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_custom_event_user"
            )
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 10)
    private TimeCategory category;

    @Column(name = "event_name", nullable = false, length = 50)
    private String eventName;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    private CustomEvent(
            User user,
            TimeCategory category,
            String eventName,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        this.user = user;
        this.category = category;
        this.eventName = eventName;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public static CustomEvent create(
            User user,
            TimeCategory category,
            String eventName,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validateTime(startAt, endAt);

        return new CustomEvent(
                user,
                category,
                eventName,
                startAt,
                endAt
        );
    }

    public void update(
            TimeCategory category,
            String eventName,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validateTime(startAt, endAt);

        this.category = category;
        this.eventName = eventName;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    private static void validateTime(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException(
                    "일정 시작 시간과 종료 시간은 필수입니다."
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "일정 종료 시간은 시작 시간보다 이후여야 합니다."
            );
        }
    }
}
