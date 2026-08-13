package org.example.nura.domain.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "daily_time_allocation",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_time_allocation_user_date",
                        columnNames = {"user_id", "date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTimeAllocation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_daily_time_allocation_user"
            )
    )
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "social_time", nullable = false)
    private Integer socialTime;

    @Column(name = "refresh_time", nullable = false)
    private Integer refreshTime;

    @Column(name = "my_time", nullable = false)
    private Integer myTime;

    @Column(name = "ai_comment", columnDefinition = "TEXT")
    private String aiComment;

    private DailyTimeAllocation(
            User user,
            LocalDate date,
            Integer socialTime,
            Integer refreshTime,
            Integer myTime,
            String aiComment
    ) {
        this.user = user;
        this.date = date;
        this.socialTime = socialTime;
        this.refreshTime = refreshTime;
        this.myTime = myTime;
        this.aiComment = aiComment;
    }

    public static DailyTimeAllocation create(
            User user,
            LocalDate date,
            Integer socialTime,
            Integer refreshTime,
            Integer myTime,
            String aiComment
    ) {
        validateMinutes(
                socialTime,
                refreshTime,
                myTime
        );

        return new DailyTimeAllocation(
                user,
                date,
                socialTime,
                refreshTime,
                myTime,
                aiComment
        );
    }

    public void updateAllocation(
            Integer socialTime,
            Integer refreshTime,
            Integer myTime,
            String aiComment
    ) {
        validateMinutes(
                socialTime,
                refreshTime,
                myTime
        );

        this.socialTime = socialTime;
        this.refreshTime = refreshTime;
        this.myTime = myTime;
        this.aiComment = aiComment;
    }

    private static void validateMinutes(
            Integer socialTime,
            Integer refreshTime,
            Integer myTime
    ) {
        if (socialTime == null
                || refreshTime == null
                || myTime == null) {
            throw new IllegalArgumentException(
                    "시간 배분 값은 null일 수 없습니다."
            );
        }

        if (socialTime < 0
                || refreshTime < 0
                || myTime < 0) {
            throw new IllegalArgumentException(
                    "시간 배분 값은 0 이상이어야 합니다."
            );
        }

        long totalMinutes =
                (long) socialTime
                        + refreshTime
                        + myTime;

        if (totalMinutes != 24 * 60) {
            throw new IllegalArgumentException(
                    "하루 시간 배분 합계는 1440분이어야 합니다."
            );
        }
    }
}
