// 이미 찬 시간 빼고 빈 시간을 계산
package org.example.nura.domain.schedule.service.plan;

import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TimeSlotCalculator {

    // 하루에서 이미 사용 중인 시간을 제외한 빈 시간 계산
    public List<TimeInterval> calculate(
            LocalDate date,
            List<TimeInterval> occupiedIntervals
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        return calculate(
                dayStart,
                dayEnd,
                occupiedIntervals
        );
    }

    // 특정 범위에서 빈 시간 계산
    public List<TimeInterval> calculate(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            List<TimeInterval> occupiedIntervals
    ) {
        List<TimeInterval> normalized =
                normalize(
                        rangeStart,
                        rangeEnd,
                        occupiedIntervals
                );

        List<TimeInterval> freeSlots =
                new ArrayList<>();

        LocalDateTime cursor =
                rangeStart;

        for (TimeInterval occupied : normalized) {

            if (occupied.startAt().isAfter(cursor)) {
                freeSlots.add(
                        new TimeInterval(
                                cursor,
                                occupied.startAt()
                        )
                );
            }

            if (occupied.endAt().isAfter(cursor)) {
                cursor = occupied.endAt();
            }
        }

        if (cursor.isBefore(rangeEnd)) {
            freeSlots.add(
                    new TimeInterval(
                            cursor,
                            rangeEnd
                    )
            );
        }

        return freeSlots;
    }

    // 범위를 벗어난 시간을 자르고 겹치는 구간 병합
    private List<TimeInterval> normalize(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            List<TimeInterval> intervals
    ) {
        if (intervals == null
                || intervals.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> sorted =
                intervals.stream()
                        .filter(interval ->
                                interval != null
                                        && interval.endAt().isAfter(rangeStart)
                                        && interval.startAt().isBefore(rangeEnd)
                        )
                        .map(interval ->
                                clip(
                                        interval,
                                        rangeStart,
                                        rangeEnd
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        TimeInterval::startAt
                                )
                        )
                        .toList();

        if (sorted.isEmpty()) {
            return List.of();
        }

        List<TimeInterval> merged =
                new ArrayList<>();

        TimeInterval current =
                sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {

            TimeInterval next =
                    sorted.get(i);

            // 겹치거나 바로 이어지는 구간은 하나로 병합
            if (!next.startAt()
                    .isAfter(current.endAt())) {

                LocalDateTime mergedEnd =
                        next.endAt().isAfter(current.endAt())
                                ? next.endAt()
                                : current.endAt();

                current =
                        new TimeInterval(
                                current.startAt(),
                                mergedEnd
                        );

                continue;
            }

            merged.add(current);
            current = next;
        }

        merged.add(current);

        return merged;
    }

    private TimeInterval clip(
            TimeInterval interval,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        LocalDateTime startAt =
                interval.startAt().isBefore(rangeStart)
                        ? rangeStart
                        : interval.startAt();

        LocalDateTime endAt =
                interval.endAt().isAfter(rangeEnd)
                        ? rangeEnd
                        : interval.endAt();

        return new TimeInterval(
                startAt,
                endAt
        );
    }
}
