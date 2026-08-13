// 목표 수면 시간 refresh time에 배치
package org.example.nura.domain.schedule.service.plan;

import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
public class SleepPlanner {

    private static final int PRE_WORK_BUFFER_MINUTES = 60;
    private static final LocalTime DEFAULT_SLEEP_START = LocalTime.MIDNIGHT;

    public TimeInterval plan(
            LocalDate date,
            ShiftType todayShiftType,
            List<TimeInterval> workIntervals,
            int targetSleepMinutes,
            List<TimeInterval> occupiedIntervals
    ) {
        if (targetSleepMinutes <= 0) {
            return null;
        }

        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        LocalDateTime preferredEnd =
                resolvePreferredSleepEnd(
                        date,
                        todayShiftType,
                        workIntervals,
                        targetSleepMinutes
                );

        LocalDateTime preferredStart =
                preferredEnd.minusMinutes(
                        targetSleepMinutes
                );

        // 오늘 범위로 제한
        preferredStart =
                preferredStart.isBefore(dayStart)
                        ? dayStart
                        : preferredStart;

        preferredEnd =
                preferredEnd.isAfter(dayEnd)
                        ? dayEnd
                        : preferredEnd;

        TimeInterval preferred =
                new TimeInterval(
                        preferredStart,
                        preferredEnd
                );

        // 선호 수면 구간에 충돌이 없으면 그대로 사용
        if (!hasOverlap(
                preferred,
                occupiedIntervals
        )) {
            return preferred;
        }

        // 충돌 시 오늘 빈 구간 중 목표 수면에 가장 가까운 구간 탐색
        return findBestAvailableSleepSlot(
                dayStart,
                dayEnd,
                targetSleepMinutes,
                occupiedIntervals,
                preferredEnd
        );
    }

    private LocalDateTime resolvePreferredSleepEnd(
            LocalDate date,
            ShiftType todayShiftType,
            List<TimeInterval> workIntervals,
            int targetSleepMinutes
    ) {
        // 근무표 없음 또는 OFF
        if (todayShiftType == null
                || todayShiftType == ShiftType.OFF) {

            return date
                    .atTime(DEFAULT_SLEEP_START)
                    .plusMinutes(targetSleepMinutes);
        }

        LocalDateTime todayStart =
                date.atStartOfDay();

        LocalDateTime tomorrowStart =
                date.plusDays(1).atStartOfDay();

        // 오늘 날짜에 시작하는 근무 구간 탐색
        TimeInterval todayWorkInterval =
                workIntervals.stream()
                        .filter(interval ->
                                !interval.startAt()
                                        .isBefore(todayStart)
                        )
                        .filter(interval ->
                                interval.startAt()
                                        .isBefore(tomorrowStart)
                        )
                        .filter(interval ->
                                !interval.startAt()
                                        .equals(todayStart)
                        )
                        .findFirst()
                        .orElse(null);

        // 실제 근무 구간을 찾지 못한 경우 기본 수면
        if (todayWorkInterval == null) {
            return date
                    .atTime(DEFAULT_SLEEP_START)
                    .plusMinutes(targetSleepMinutes);
        }

        // 근무 시작 전 여유 시간 확보
        return todayWorkInterval
                .startAt()
                .minusMinutes(
                        PRE_WORK_BUFFER_MINUTES
                );
    }

    private TimeInterval findBestAvailableSleepSlot(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            int targetSleepMinutes,
            List<TimeInterval> occupiedIntervals,
            LocalDateTime preferredEnd
    ) {
        List<TimeInterval> freeSlots =
                calculateFreeSlots(
                        dayStart,
                        dayEnd,
                        occupiedIntervals
                );

        // 목표 수면을 전부 확보할 수 있는 구간 우선
        TimeInterval fullSleep =
                freeSlots.stream()
                        .filter(slot ->
                                durationMinutes(slot)
                                        >= targetSleepMinutes
                        )
                        .min(
                                Comparator.comparingLong(
                                        slot ->
                                                distanceFromPreferredEnd(
                                                        slot,
                                                        preferredEnd
                                                )
                                )
                        )
                        .map(slot -> {

                            LocalDateTime endAt =
                                    slot.endAt();

                            LocalDateTime startAt =
                                    endAt.minusMinutes(
                                            targetSleepMinutes
                                    );

                            return new TimeInterval(
                                    startAt,
                                    endAt
                            );
                        })
                        .orElse(null);

        if (fullSleep != null) {
            return fullSleep;
        }

        // 목표 수면 확보가 불가능하면 가장 긴 빈 구간 사용
        return freeSlots.stream()
                .max(
                        Comparator.comparingLong(
                                this::durationMinutes
                        )
                )
                .orElse(null);
    }

    private List<TimeInterval> calculateFreeSlots(
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            List<TimeInterval> occupiedIntervals
    ) {
        List<TimeInterval> sorted =
                occupiedIntervals.stream()
                        .filter(interval ->
                                interval.endAt().isAfter(dayStart)
                                        && interval.startAt().isBefore(dayEnd)
                        )
                        .sorted(
                                Comparator.comparing(
                                        TimeInterval::startAt
                                )
                        )
                        .toList();

        java.util.ArrayList<TimeInterval> freeSlots =
                new java.util.ArrayList<>();

        LocalDateTime cursor =
                dayStart;

        for (TimeInterval occupied : sorted) {

            LocalDateTime startAt =
                    occupied.startAt().isBefore(dayStart)
                            ? dayStart
                            : occupied.startAt();

            LocalDateTime endAt =
                    occupied.endAt().isAfter(dayEnd)
                            ? dayEnd
                            : occupied.endAt();

            if (startAt.isAfter(cursor)) {
                freeSlots.add(
                        new TimeInterval(
                                cursor,
                                startAt
                        )
                );
            }

            if (endAt.isAfter(cursor)) {
                cursor = endAt;
            }
        }

        if (cursor.isBefore(dayEnd)) {
            freeSlots.add(
                    new TimeInterval(
                            cursor,
                            dayEnd
                    )
            );
        }

        return freeSlots;
    }

    private boolean hasOverlap(
            TimeInterval target,
            List<TimeInterval> intervals
    ) {
        return intervals.stream()
                .anyMatch(interval ->
                        target.startAt()
                                .isBefore(interval.endAt())
                                && target.endAt()
                                .isAfter(interval.startAt())
                );
    }

    private long durationMinutes(
            TimeInterval interval
    ) {
        return ChronoUnit.MINUTES.between(
                interval.startAt(),
                interval.endAt()
        );
    }

    private long distanceFromPreferredEnd(
            TimeInterval interval,
            LocalDateTime preferredEnd
    ) {
        return Math.abs(
                ChronoUnit.MINUTES.between(
                        interval.endAt(),
                        preferredEnd
                )
        );
    }
}
