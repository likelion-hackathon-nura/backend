// 근무 시간 오늘 하루 기준 실제 시간 구간으로 구성
package org.example.nura.domain.schedule.service.plan;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkIntervalCalculator {

    private final DutyScheduleRepository dutyScheduleRepository;

    public List<TimeInterval> calculate(
            Long userId,
            LocalDate date
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        List<TimeInterval> intervals =
                new ArrayList<>();

        // 전날 근무 조회
        dutyScheduleRepository
                .findByUserIdAndDate(
                        userId,
                        date.minusDays(1)
                )
                .map(this::toWorkInterval)
                .map(interval ->
                        clipToDay(
                                interval,
                                dayStart,
                                dayEnd
                        )
                )
                .ifPresent(intervals::add);

        // 오늘 근무 조회
        dutyScheduleRepository
                .findByUserIdAndDate(
                        userId,
                        date
                )
                .filter(schedule ->
                        schedule.getShiftType()
                                != ShiftType.OFF
                )
                .map(this::toWorkInterval)
                .map(interval ->
                        clipToDay(
                                interval,
                                dayStart,
                                dayEnd
                        )
                )
                .ifPresent(intervals::add);

        return intervals;
    }

    private TimeInterval toWorkInterval(
            DutySchedule schedule
    ) {
        LocalDate date =
                schedule.getDate();

        LocalDateTime startAt =
                date.atTime(
                        schedule.getStartTime()
                );

        LocalDateTime endAt =
                date.atTime(
                        schedule.getEndTime()
                );

        // 자정을 넘기는 근무 처리
        if (!endAt.isAfter(startAt)) {
            endAt =
                    endAt.plusDays(1);
        }

        return new TimeInterval(
                startAt,
                endAt
        );
    }

    private TimeInterval clipToDay(
            TimeInterval interval,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        if (interval == null) {
            return null;
        }

        LocalDateTime startAt =
                interval.startAt().isBefore(dayStart)
                        ? dayStart
                        : interval.startAt();

        LocalDateTime endAt =
                interval.endAt().isAfter(dayEnd)
                        ? dayEnd
                        : interval.endAt();

        // 오늘과 실제로 겹치지 않음
        if (!endAt.isAfter(startAt)) {
            return null;
        }

        return new TimeInterval(
                startAt,
                endAt
        );
    }
}
