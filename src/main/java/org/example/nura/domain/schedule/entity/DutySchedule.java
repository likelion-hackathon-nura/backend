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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.nura.domain.schedule.entity.enums.DutyScheduleSource;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.user.entity.User;
import org.example.nura.global.common.BaseTimeEntity;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Entity
@Table(
        name = "duty_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_duty_schedule_user_date",
                        columnNames = {"user_id", "date"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DutySchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "duty_schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_duty_schedule_user"
            )
    )
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false, length = 10)
    private ShiftType shiftType;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 10)
    private DutyScheduleSource source;

    private DutySchedule(
            User user,
            LocalDate date,
            ShiftType shiftType,
            LocalTime startTime,
            LocalTime endTime,
            DutyScheduleSource source
    ) {
        this.user = user;
        this.date = date;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.source = source;
    }

    public static DutySchedule create(
            User user,
            LocalDate date,
            ShiftType shiftType,
            LocalTime startTime,
            LocalTime endTime,
            DutyScheduleSource source
    ) {
        validateShiftTime(
                shiftType,
                startTime,
                endTime
        );

        return new DutySchedule(
                user,
                date,
                shiftType,
                startTime,
                endTime,
                source
        );
    }

    public void update(
            ShiftType shiftType,
            LocalTime startTime,
            LocalTime endTime,
            DutyScheduleSource source
    ) {
        validateShiftTime(
                shiftType,
                startTime,
                endTime
        );

        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.source = source;
    }

    private static void validateShiftTime(
            ShiftType shiftType,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (shiftType == ShiftType.OFF) {
            if (startTime != null || endTime != null) {
                throw new IllegalArgumentException(
                        "OFF 근무는 시작 시간과 종료 시간을 가질 수 없습니다."
                );
            }
            return;
        }

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "근무일은 시작 시간과 종료 시간이 필요합니다."
            );
        }

        if (startTime.equals(endTime)) {
            throw new IllegalArgumentException(
                    "근무 시작 시간과 종료 시간은 같을 수 없습니다."
            );
        }
    }
}
