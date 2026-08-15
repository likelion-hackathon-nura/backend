package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanItem;
import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RefreshPlanFeedbackBalancer {

    private static final int MIN_REFRESH_ACTIVITY_MINUTES = 5;
    private static final int ADJUSTMENT_STEP_MINUTES = 5;
    private static final int MAX_SHIFT_MINUTES = 15;

    public RefreshPlanAiResponse balance(
            RefreshPlanAiRequest request,
            RefreshPlanAiResponse response,
            int availablePoolMinutes
    ) {
        if (response == null) {
            return null;
        }

        List<RefreshComponent> components =
                buildComponents(
                        request.availableSlots(),
                        response.refreshPlan()
                );

        if (components.isEmpty()
                || availablePoolMinutes <= 0) {
            return response;
        }

        int currentRefreshMinutes =
                components.stream()
                        .mapToInt(component -> component.currentMinutes)
                        .sum();

        if (currentRefreshMinutes <= 0) {
            return response;
        }

        int balanceScore =
                request.balanceScore() == null
                        ? 0
                        : request.balanceScore();

        if (Math.abs(balanceScore) < 2) {
            return response;
        }

        int shiftMinutes =
                Math.min(
                        MAX_SHIFT_MINUTES,
                        Math.abs(balanceScore) * ADJUSTMENT_STEP_MINUTES
                );

        int targetRefreshMinutes =
                currentRefreshMinutes
                        + (balanceScore > 0
                        ? shiftMinutes
                        : -shiftMinutes);

        int minRefreshMinutes =
                components.stream()
                        .mapToInt(component -> component.minMinutes)
                        .sum();
        int maxRefreshMinutes =
                components.stream()
                        .mapToInt(component -> component.maxMinutes)
                        .sum();

        targetRefreshMinutes =
                Math.max(
                        minRefreshMinutes,
                        Math.min(
                                Math.min(
                                        maxRefreshMinutes,
                                        availablePoolMinutes
                                ),
                                targetRefreshMinutes
                        )
                );

        if (targetRefreshMinutes == currentRefreshMinutes) {
            return response;
        }

        Map<Integer, Integer> adjustedDurations =
                redistribute(
                        components,
                        targetRefreshMinutes
                );

        List<RefreshPlanItem> adjustedItems =
                new ArrayList<>();

        for (RefreshComponent component : components) {
            adjustedItems.add(
                    new RefreshPlanItem(
                            component.activityType,
                            adjustedDurations.get(component.index),
                            component.slotId
                    )
            );
        }

        return new RefreshPlanAiResponse(
                adjustedItems,
                response.skinRecovery()
        );
    }

    private List<RefreshComponent> buildComponents(
            List<AvailableSlotContext> availableSlots,
            List<RefreshPlanItem> refreshPlan
    ) {
        Map<String, Integer> slotDurationMap =
                new HashMap<>();

        if (availableSlots != null) {
            for (AvailableSlotContext slot : availableSlots) {
                slotDurationMap.put(
                        slot.slotId(),
                        (int) slot.durationMinutes()
                );
            }
        }

        List<RefreshComponent> components =
                new ArrayList<>();

        if (refreshPlan == null) {
            return components;
        }

        for (int i = 0; i < refreshPlan.size(); i++) {
            RefreshPlanItem item =
                    refreshPlan.get(i);

            Integer slotDuration =
                    slotDurationMap.get(
                            item.preferredSlotId()
                    );

            if (slotDuration == null) {
                continue;
            }

            components.add(
                    new RefreshComponent(
                            i,
                            item.activityType(),
                            item.preferredSlotId(),
                            item.durationMinutes(),
                            MIN_REFRESH_ACTIVITY_MINUTES,
                            slotDuration
                    )
            );
        }

        return components;
    }

    private Map<Integer, Integer> redistribute(
            List<RefreshComponent> components,
            int targetRefreshMinutes
    ) {
        Map<Integer, Integer> durations =
                new HashMap<>();

        double currentRefreshMinutes =
                components.stream()
                        .mapToInt(component -> component.currentMinutes)
                        .sum();

        double factor =
                targetRefreshMinutes / currentRefreshMinutes;

        for (RefreshComponent component : components) {
            int baseDuration =
                    (int) Math.floor(
                            component.currentMinutes * factor
                    );
            int clampedDuration =
                    Math.max(
                            component.minMinutes,
                            Math.min(
                                    component.maxMinutes,
                                    baseDuration
                            )
                    );

            durations.put(
                    component.index,
                    clampedDuration
            );
        }

        int currentTotal =
                durations.values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

        int remainder =
                targetRefreshMinutes - currentTotal;

        if (remainder > 0) {
            List<RefreshComponent> ordered =
                    components.stream()
                            .sorted(
                                    Comparator
                                            .comparingInt(
                                                    (RefreshComponent c) ->
                                                            c.currentMinutes
                                            )
                                            .reversed()
                                            .thenComparing(
                                                    c -> c.index
                                            )
                            )
                            .toList();

            while (remainder > 0) {
                boolean changed = false;

                for (RefreshComponent component : ordered) {
                    int currentDuration =
                            durations.get(
                                    component.index
                            );

                    if (currentDuration >= component.maxMinutes) {
                        continue;
                    }

                    durations.put(
                            component.index,
                            currentDuration + 1
                    );
                    remainder--;
                    changed = true;

                    if (remainder == 0) {
                        break;
                    }
                }

                if (!changed) {
                    break;
                }
            }
        } else if (remainder < 0) {
            List<RefreshComponent> ordered =
                    components.stream()
                            .sorted(
                                    Comparator
                                            .comparingInt(
                                                    (RefreshComponent c) ->
                                                            c.currentMinutes
                                            )
                                            .thenComparing(
                                                    c -> c.index
                                            )
                            )
                            .toList();

            while (remainder < 0) {
                boolean changed = false;

                for (RefreshComponent component : ordered) {
                    int currentDuration =
                            durations.get(
                                    component.index
                            );

                    if (currentDuration <= component.minMinutes) {
                        continue;
                    }

                    durations.put(
                            component.index,
                            currentDuration - 1
                    );
                    remainder++;
                    changed = true;

                    if (remainder == 0) {
                        break;
                    }
                }

                if (!changed) {
                    break;
                }
            }
        }

        return durations;
    }

    private record RefreshComponent(
            int index,
            org.example.nura.domain.user.entity.enums.RestActivityType activityType,
            String slotId,
            int currentMinutes,
            int minMinutes,
            int maxMinutes
    ) {
    }
}
