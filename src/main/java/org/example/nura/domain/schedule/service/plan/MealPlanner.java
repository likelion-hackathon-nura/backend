// 온보딩에서 받은 거에 따라서 식사 시간을 refresh time에 배치
package org.example.nura.domain.schedule.service.plan;

import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.user.entity.enums.MealPattern;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class MealPlanner {

    private static final int MEAL_DURATION_MINUTES = 30;
    private static final int WORK_BUFFER_MINUTES = 90;

    private static final LocalTime FIRST_MEAL_START =
            LocalTime.of(11, 0);

    private static final LocalTime FIRST_MEAL_END =
            LocalTime.of(14, 0);

    private static final LocalTime SECOND_MEAL_START =
            LocalTime.of(17, 0);

    private static final LocalTime SECOND_MEAL_END =
            LocalTime.of(21, 0);

    public List<TimeInterval> plan(
            LocalDate date,
            MealPattern mealPattern,
            List<TimeInterval> workIntervals,
            List<TimeInterval> occupiedIntervals
    ) {
        int mealCount =
                resolveMealCount(mealPattern);

        List<TimeInterval> meals =
                new ArrayList<>();

        List<TimeInterval> occupied =
                new ArrayList<>(occupiedIntervals);

        // 첫 번째 식사 배치
        TimeInterval firstMeal =
                findMealSlot(
                        date,
                        FIRST_MEAL_START,
                        FIRST_MEAL_END,
                        workIntervals,
                        occupied
                );

        // 선호 시간대에 식사를 배치하지 못하면 퇴근 이후 빈 시간에 배치
        if (firstMeal == null) {
            firstMeal =
                    findFallbackMealSlot(
                            date,
                            workIntervals,
                            occupied
                    );
        }

        if (firstMeal != null) {
            meals.add(firstMeal);
            occupied.add(firstMeal);
        }

        if (mealCount == 1) {
            return meals;
        }

        // 두 번째 식사 배치
        TimeInterval secondMeal =
                findMealSlot(
                        date,
                        SECOND_MEAL_START,
                        SECOND_MEAL_END,
                        workIntervals,
                        occupied
                );

        if (secondMeal != null) {
            meals.add(secondMeal);
            occupied.add(secondMeal);
        }

        return meals;
    }

    private TimeInterval findFallbackMealSlot(
            LocalDate date,
            List<TimeInterval> workIntervals,
            List<TimeInterval> occupiedIntervals
    ) {
        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        LocalDateTime fallbackStart =
                workIntervals.stream()
                        .filter(interval ->
                                interval.startAt()
                                        .toLocalDate()
                                        .equals(date)
                        )
                        .map(TimeInterval::endAt)
                        .max(Comparator.naturalOrder())
                        .orElse(
                                date.atTime(FIRST_MEAL_START)
                        );

        List<TimeInterval> freeSlots =
                calculateFreeSlots(
                        fallbackStart,
                        dayEnd,
                        occupiedIntervals
                );

        freeSlots =
                excludePreWorkBufferTime(
                        freeSlots,
                        workIntervals
                );

        return freeSlots.stream()
                .filter(slot ->
                        !slot.startAt()
                                .plusMinutes(MEAL_DURATION_MINUTES)
                                .isAfter(slot.endAt())
                )
                .findFirst()
                .map(slot ->
                        new TimeInterval(
                                slot.startAt(),
                                slot.startAt()
                                        .plusMinutes(
                                                MEAL_DURATION_MINUTES
                                        )
                        )
                )
                .orElse(null);
    }

    private int resolveMealCount(
            MealPattern mealPattern
    ) {
        if (mealPattern == null) {
            return 1;
        }

        return switch (mealPattern) {
            case REGULAR, SOMETIMES_SKIP -> 2;
            case OFTEN_SKIP -> 1;
        };
    }

    private TimeInterval findMealSlot(
            LocalDate date,
            LocalTime preferredStart,
            LocalTime preferredEnd,
            List<TimeInterval> workIntervals,
            List<TimeInterval> occupiedIntervals
    ) {
        LocalDateTime rangeStart =
                date.atTime(preferredStart);

        LocalDateTime rangeEnd =
                date.atTime(preferredEnd);

        List<TimeInterval> freeSlots =
                calculateFreeSlots(
                        rangeStart,
                        rangeEnd,
                        occupiedIntervals
                );

        freeSlots =
                excludePreWorkBufferTime(
                        freeSlots,
                        workIntervals
                );

        return freeSlots.stream()
                .filter(slot ->
                        !slot.startAt()
                                .plusMinutes(MEAL_DURATION_MINUTES)
                                .isAfter(slot.endAt())
                )
                .min(
                        Comparator.comparing(
                                TimeInterval::startAt
                        )
                )
                .map(slot ->
                        new TimeInterval(
                                slot.startAt(),
                                slot.startAt()
                                        .plusMinutes(
                                                MEAL_DURATION_MINUTES
                                        )
                        )
                )
                .orElse(null);
    }

    private List<TimeInterval> excludePreWorkBufferTime(
            List<TimeInterval> intervals,
            List<TimeInterval> workIntervals
    ) {
        if (workIntervals.isEmpty()) {
            return intervals;
        }

        List<TimeInterval> availableIntervals =
                intervals;

        for (TimeInterval workInterval : workIntervals) {
            LocalDateTime blockedStart =
                    workInterval.startAt()
                            .minusMinutes(WORK_BUFFER_MINUTES);

            LocalDateTime blockedEnd =
                    workInterval.startAt();

            availableIntervals =
                    availableIntervals.stream()
                            .flatMap(interval ->
                                    excludeBlockedInterval(
                                            interval,
                                            blockedStart,
                                            blockedEnd
                                    ).stream()
                            )
                            .toList();
        }

        return availableIntervals;
    }

    private List<TimeInterval> excludeBlockedInterval(
            TimeInterval interval,
            LocalDateTime blockedStart,
            LocalDateTime blockedEnd
    ) {
        if (!interval.startAt().isBefore(blockedEnd)
                || !interval.endAt().isAfter(blockedStart)) {
            return List.of(interval);
        }

        List<TimeInterval> remaining =
                new ArrayList<>();

        if (interval.startAt().isBefore(blockedStart)) {
            remaining.add(
                    new TimeInterval(
                            interval.startAt(),
                            blockedStart
                    )
            );
        }

        if (interval.endAt().isAfter(blockedEnd)) {
            remaining.add(
                    new TimeInterval(
                            blockedEnd,
                            interval.endAt()
                    )
            );
        }

        return remaining;
    }

    private LocalDateTime capBeforeWorkStart(
            LocalDate date,
            LocalTime preferredEnd,
            List<TimeInterval> workIntervals
    ) {
        LocalDateTime endAt =
                date.atTime(preferredEnd);

        LocalDateTime workStart =
                workIntervals.stream()
                        .map(TimeInterval::startAt)
                        .filter(startAt ->
                                startAt.toLocalDate().equals(date)
                        )
                        .min(Comparator.naturalOrder())
                        .orElse(null);

        if (workStart == null) {
            return endAt;
        }

        LocalDateTime refreshLimit =
                workStart.minusMinutes(90);

        return endAt.isAfter(refreshLimit)
                ? refreshLimit
                : endAt;
    }

    private List<TimeInterval> calculateFreeSlots(
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            List<TimeInterval> occupiedIntervals
    ) {
        List<TimeInterval> sorted =
                occupiedIntervals.stream()
                        .filter(interval ->
                                interval.endAt()
                                        .isAfter(rangeStart)
                                        && interval.startAt()
                                        .isBefore(rangeEnd)
                        )
                        .sorted(
                                Comparator.comparing(
                                        TimeInterval::startAt
                                )
                        )
                        .toList();

        List<TimeInterval> freeSlots =
                new ArrayList<>();

        LocalDateTime cursor =
                rangeStart;

        for (TimeInterval occupied : sorted) {

            LocalDateTime occupiedStart =
                    occupied.startAt()
                            .isBefore(rangeStart)
                            ? rangeStart
                            : occupied.startAt();

            LocalDateTime occupiedEnd =
                    occupied.endAt()
                            .isAfter(rangeEnd)
                            ? rangeEnd
                            : occupied.endAt();

            if (occupiedStart.isAfter(cursor)) {
                freeSlots.add(
                        new TimeInterval(
                                cursor,
                                occupiedStart
                        )
                );
            }

            if (occupiedEnd.isAfter(cursor)) {
                cursor = occupiedEnd;
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
}
