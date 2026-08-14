package org.example.nura.domain.schedule.service.plan;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.dto.plan.RefreshAllocationResult;
import org.example.nura.domain.schedule.entity.CustomEvent;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.domain.schedule.repository.CustomEventRepository;
import org.example.nura.domain.schedule.service.refresh.AvailableSlotMapper;
import org.example.nura.domain.schedule.service.refresh.DailyRefreshAiService;
import org.example.nura.domain.schedule.service.refresh.RefreshPlanAllocator;
import org.example.nura.domain.schedule.service.refresh.SkinRecoverySlotResolver;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyPlanGenerationService {

    private final WorkIntervalCalculator workIntervalCalculator;
    private final SleepPlanner sleepPlanner;
    private final MealPlanner mealPlanner;
    private final TimeSlotCalculator timeSlotCalculator;

    private final AvailableSlotMapper availableSlotMapper;
    private final DailyRefreshAiService dailyRefreshAiService;
    private final RefreshPlanAllocator refreshPlanAllocator;

    private final CustomEventRepository customEventRepository;

    private final SkinRecoverySlotResolver skinRecoverySlotResolver;

    private final DailyPlanValidator dailyPlanValidator;

    public List<PlannedTimeBlock> generate(
            Long userId,
            DailyPlanContext context
    ) {
        LocalDate date = context.date();

        List<PlannedTimeBlock> blocks =
                new ArrayList<>();

        List<TimeInterval> occupied =
                new ArrayList<>();

        // 근무 배치
        List<TimeInterval> workIntervals =
                workIntervalCalculator.calculate(
                        userId,
                        date
                );

        workIntervals.forEach(interval -> {
            blocks.add(
                    new PlannedTimeBlock(
                            TimeCategory.SOCIAL,
                            "근무",
                            interval.startAt(),
                            interval.endAt(),
                            TimeBlockSource.SCHEDULE,
                            null
                    )
            );

            occupied.add(interval);
        });

        // 기존 사용자 일정 배치
        List<CustomEvent> customEvents =
                readCustomEvents(
                        userId,
                        date
                );

        for (CustomEvent event : customEvents) {

            TimeInterval interval =
                    clipCustomEventToDay(
                            event,
                            date
                    );

            blocks.add(
                    new PlannedTimeBlock(
                            event.getCategory(),
                            event.getEventName(),
                            interval.startAt(),
                            interval.endAt(),
                            TimeBlockSource.MANUAL,
                            event
                    )
            );

            occupied.add(interval);
        }

        TimeInterval sleepInterval =
                sleepPlanner.plan(
                        date,
                        context.todayShiftType(),
                        workIntervals,
                        context.targetSleepMinutes(),
                        occupied
                );

        if (sleepInterval != null) {
            blocks.add(
                    new PlannedTimeBlock(
                            TimeCategory.REFRESH,
                            "수면",
                            sleepInterval.startAt(),
                            sleepInterval.endAt(),
                            TimeBlockSource.AUTO,
                            null
                    )
            );

            occupied.add(sleepInterval);
        }

        // 식사 배치
        List<TimeInterval> mealIntervals =
                mealPlanner.plan(
                        date,
                        context.mealPattern(),
                        workIntervals,
                        occupied
                );

        for (TimeInterval mealInterval : mealIntervals) {
            blocks.add(
                    new PlannedTimeBlock(
                            TimeCategory.REFRESH,
                            "식사",
                            mealInterval.startAt(),
                            mealInterval.endAt(),
                            TimeBlockSource.AUTO,
                            null
                    )
            );

            occupied.add(mealInterval);
        }

        // 추가 회복 활동을 배치할 수 있는 시간 계산
        List<TimeInterval> availableIntervals =
                timeSlotCalculator.calculate(
                        date,
                        occupied
                );

        availableIntervals =
                excludePreWorkRefreshTime(
                        date,
                        workIntervals,
                        availableIntervals
                );

        List<AvailableSlotContext> availableSlots =
                availableSlotMapper.map(
                        availableIntervals
                );

        List<AvailableSlotContext> skinRecoveryAvailableSlots =
                skinRecoverySlotResolver.resolve(
                        date,
                        context.todayShiftType(),
                        workIntervals,
                        availableSlots
                );

        // AI 회복 계획 요청
        RefreshPlanAiRequest aiRequest =
                createRefreshAiRequest(
                        context,
                        availableSlots,
                        skinRecoveryAvailableSlots
                );

        RefreshPlanAiResponse aiResponse =
                dailyRefreshAiService.recommend(
                        aiRequest
                );

        // AI 추천을 실제 시간으로 배치
        RefreshAllocationResult refreshAllocation =
                refreshPlanAllocator.allocate(
                        aiRequest,
                        aiResponse
                );

        // 온보딩 RestActivity 기반 회복 블록
        refreshAllocation.activities()
                .forEach(activity -> {

                    TimeInterval interval =
                            activity.interval();

                    blocks.add(
                            new PlannedTimeBlock(
                                    TimeCategory.REFRESH,
                                    resolveActivityLabel(
                                            activity.activityType()
                                    ),
                                    interval.startAt(),
                                    interval.endAt(),
                                    TimeBlockSource.AUTO,
                                    null
                            )
                    );

                    occupied.add(interval);
                });

        // 피부 회복 블록
        if (refreshAllocation.skinRecovery() != null) {

            TimeInterval interval =
                    refreshAllocation
                            .skinRecovery()
                            .interval();

            blocks.add(
                    new PlannedTimeBlock(
                            TimeCategory.REFRESH,
                            "피부 회복",
                            interval.startAt(),
                            interval.endAt(),
                            TimeBlockSource.AUTO,
                            null
                    )
            );

            occupied.add(interval);
        }

        // 마지막 남은 시간은 MY
        List<TimeInterval> myIntervals =
                timeSlotCalculator.calculate(
                        date,
                        occupied
                );

        for (TimeInterval myInterval : myIntervals) {
            blocks.add(
                    new PlannedTimeBlock(
                            TimeCategory.MY,
                            null,
                            myInterval.startAt(),
                            myInterval.endAt(),
                            TimeBlockSource.AUTO,
                            null
                    )
            );
        }

        List<PlannedTimeBlock> result =
                blocks.stream()
                        .sorted(
                                Comparator.comparing(
                                        PlannedTimeBlock::startAt
                                )
                        )
                        .toList();

        // 최종 하루 설계 검증
        dailyPlanValidator.validate(
                date,
                result
        );

        return result;
    }

    private RefreshPlanAiRequest createRefreshAiRequest(
            DailyPlanContext context,
            List<AvailableSlotContext> availableSlots,
            List<AvailableSlotContext> skinRecoveryAvailableSlots
    ) {
        return new RefreshPlanAiRequest(
                context.date(),
                context.todayShiftType(),
                context.consecutiveWorkDays(),
                context.consecutiveNightShiftDays(),
                context.targetSleepMinutes(),
                context.mealPattern(),
                context.restActivities(),
                context.sensitivityLevel(),
                context.skinType(),
                context.skinConcerns(),
                context.previousCheckin(),
                context.previousRecoveryRoutineCompleted(),
                availableSlots,
                skinRecoveryAvailableSlots
        );
    }

    private List<CustomEvent> readCustomEvents(
            Long userId,
            LocalDate date
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1)
                        .atStartOfDay();

        return customEventRepository
                .findOverlappingEvents(
                        userId,
                        dayStart,
                        dayEnd
                );
    }

    private List<TimeInterval> excludePreWorkRefreshTime(
            LocalDate date,
            List<TimeInterval> workIntervals,
            List<TimeInterval> intervals
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        LocalDateTime workStart =
                workIntervals.stream()
                        .map(TimeInterval::startAt)
                        .filter(startAt ->
                                startAt.isAfter(dayStart)
                                        && startAt.isBefore(dayEnd)
                        )
                        .min(LocalDateTime::compareTo)
                        .orElse(null);

        if (workStart == null) {
            return intervals;
        }

        LocalDateTime refreshLimit =
                workStart.minusMinutes(90);

        return intervals.stream()
                .map(interval -> {
                    if (!interval.startAt().isBefore(refreshLimit)
                            && interval.startAt().isBefore(workStart)) {
                        return null;
                    }

                    if (interval.startAt().isBefore(refreshLimit)
                            && interval.endAt().isAfter(refreshLimit)) {
                        return new TimeInterval(
                                interval.startAt(),
                                refreshLimit
                        );
                    }

                    return interval;
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private TimeInterval clipCustomEventToDay(
            CustomEvent event,
            LocalDate date
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1)
                        .atStartOfDay();

        LocalDateTime startAt =
                event.getStartAt()
                        .isBefore(dayStart)
                        ? dayStart
                        : event.getStartAt();

        LocalDateTime endAt =
                event.getEndAt()
                        .isAfter(dayEnd)
                        ? dayEnd
                        : event.getEndAt();

        return new TimeInterval(
                startAt,
                endAt
        );
    }

    private String resolveActivityLabel(
            RestActivityType activityType
    ) {
        return switch (activityType) {
            case NAP_SLEEP -> "낮잠";
            case BATH -> "목욕";
            case READING_STUDY -> "독서/공부";
            case EXERCISE_STRETCH -> "운동/스트레칭";
            case PET_CARE -> "반려동물 돌보기";
            case WALK_CAFE -> "산책/카페";
            case MEDITATION -> "명상";
            case OTHER -> "휴식";
        };
    }
}
