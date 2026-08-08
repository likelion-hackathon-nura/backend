package org.example.nura.domain.report.entity;

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
        name = "weekly_report",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_weekly_report_user_week_start",
                        columnNames = {"user_id", "week_start_date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "weekly_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_weekly_report_user"
            )
    )
    private User user;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(
            name = "ai_comment",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String aiComment;

    private WeeklyReport(
            User user,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String aiComment
    ) {
        this.user = user;
        this.weekStartDate = weekStartDate;
        this.weekEndDate = weekEndDate;
        this.aiComment = aiComment;
    }

    public static WeeklyReport create(
            User user,
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String aiComment
    ) {
        validatePeriod(weekStartDate, weekEndDate);

        return new WeeklyReport(
                user,
                weekStartDate,
                weekEndDate,
                aiComment
        );
    }

    private static void validatePeriod(
            LocalDate weekStartDate,
            LocalDate weekEndDate
    ) {
        if (weekStartDate == null || weekEndDate == null) {
            throw new IllegalArgumentException(
                    "주간 리포트의 시작일과 종료일은 필수입니다."
            );
        }

        if (weekEndDate.isBefore(weekStartDate)) {
            throw new IllegalArgumentException(
                    "주간 리포트 종료일은 시작일보다 이전일 수 없습니다."
            );
        }
    }
}
