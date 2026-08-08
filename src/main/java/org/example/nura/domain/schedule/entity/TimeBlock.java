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
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.global.common.BaseTimeEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "time_block")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "block_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "allocation_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_time_block_allocation"
            )
    )
    private DailyTimeAllocation allocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "custom_event_id",
            foreignKey = @ForeignKey(
                    name = "fk_time_block_custom_event"
            )
    )
    private CustomEvent customEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 10)
    private TimeCategory category;

    @Column(name = "label", length = 50)
    private String label;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 15)
    private TimeBlockSource source;

    @Column(name = "completed")
    private Boolean completed;

    private TimeBlock(
            DailyTimeAllocation allocation,
            CustomEvent customEvent,
            TimeCategory category,
            String label,
            LocalDateTime startAt,
            LocalDateTime endAt,
            TimeBlockSource source,
            Boolean completed
    ) {
        this.allocation = allocation;
        this.customEvent = customEvent;
        this.category = category;
        this.label = label;
        this.startAt = startAt;
        this.endAt = endAt;
        this.source = source;
        this.completed = completed;
    }

    public static TimeBlock create(
            DailyTimeAllocation allocation,
            CustomEvent customEvent,
            TimeCategory category,
            String label,
            LocalDateTime startAt,
            LocalDateTime endAt,
            TimeBlockSource source,
            Boolean completed
    ) {
        validateTime(startAt, endAt);
        validateSource(source, customEvent);

        return new TimeBlock(
                allocation,
                customEvent,
                category,
                label,
                startAt,
                endAt,
                source,
                completed
        );
    }

    public void updateTime(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        validateTime(startAt, endAt);

        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void updateCompleted(Boolean completed) {
        this.completed = completed;
    }

    private static void validateTime(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException(
                    "타임 블록의 시작 시간과 종료 시간은 필수입니다."
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "타임 블록의 종료 시간은 시작 시간보다 이후여야 합니다."
            );
        }
    }

    private static void validateSource(
            TimeBlockSource source,
            CustomEvent customEvent
    ) {
        if (source == TimeBlockSource.MANUAL && customEvent == null) {
            throw new IllegalArgumentException(
                    "수동 일정 블록은 CustomEvent가 필요합니다."
            );
        }

        if (source != TimeBlockSource.MANUAL && customEvent != null) {
            throw new IllegalArgumentException(
                    "자동 또는 근무 블록은 CustomEvent를 가질 수 없습니다."
            );
        }
    }
}
