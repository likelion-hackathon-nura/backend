// 온보딩에서 받은 거에 따라서 식시 사긴 refresh time에 배치
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
                capBeforeWorkStart(
                        date,
                        preferredEnd,
                        workIntervals
                );

        if (!rangeStart.isBefore(rangeEnd)) {
            return null;
        }

        List<TimeInterval> freeSlots =
                calculateFreeSlots(
                        rangeStart,
                        rangeEnd,
                        occupiedIntervals
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
                workStart.minusHours(1);

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
