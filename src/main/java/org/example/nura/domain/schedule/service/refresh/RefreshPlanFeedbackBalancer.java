package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanItem;
import org.example.nura.domain.schedule.dto.ai.SkinRecoveryPlan;
import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RefreshPlanFeedbackBalancer {

    private static final int ADJUSTMENT_STEP_MINUTES = 15;
    private static final int MIN_REFRESH_ACTIVITY_MINUTES = 1;
    private static final int MIN_SKIN_RECOVERY_MINUTES = 15;

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
                        request,
                        response
                );

        if (components.isEmpty()) {
            return response;
        }

        int currentRefreshMinutes =
                components.stream()
                        .mapToInt(component ->
                                component.currentMinutes
                        )
                        .sum();

        if (currentRefreshMinutes <= 0
                || availablePoolMinutes <= 0) {
            return response;
        }

        int currentMyMinutes =
                Math.max(
                        0,
                        availablePoolMinutes - currentRefreshMinutes
                );

        int refreshWeight =
                Math.max(
                        1,
                        currentRefreshMinutes
                                + toAdjustmentMinutes(
                                request.refreshAdjustment()
                        )
                );
        int myWeight =
                Math.max(
                        1,
                        currentMyMinutes
                                + toAdjustmentMinutes(
                                request.myAdjustment()
                        )
                );

        int targetRefreshMinutes =
                (int) Math.round(
                        (double) availablePoolMinutes
                                * refreshWeight
                                / (refreshWeight + myWeight)
                );

        int minRefreshMinutes =
                components.stream()
                        .mapToInt(component ->
                                component.minMinutes
                        )
                        .sum();
        int maxRefreshMinutes =
                components.stream()
                        .mapToInt(component ->
                                component.maxMinutes
                        )
                        .sum();

        targetRefreshMinutes =
                Math.max(
                        minRefreshMinutes,
                        Math.min(
                                maxRefreshMinutes,
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
            if (component.skinRecovery) {
                continue;
            }

            adjustedItems.add(
                    new RefreshPlanItem(
                            component.activityType,
                            adjustedDurations.get(
                                    component.index
                            ),
                            component.slotId
                    )
            );
        }

        SkinRecoveryPlan adjustedSkinRecovery =
                response.skinRecovery() == null
                        ? null
                        : new SkinRecoveryPlan(
                        response.skinRecovery().enabled(),
                        findAdjustedSkinRecoveryDuration(
                                components,
                                adjustedDurations,
                                response.skinRecovery().durationMinutes()
                        ),
                        response.skinRecovery().preferredSlotId()
                );

        return new RefreshPlanAiResponse(
                adjustedItems,
                adjustedSkinRecovery
        );
    }

    private Map<Integer, Integer> redistribute(
            List<RefreshComponent> components,
            int targetRefreshMinutes
    ) {
        Map<Integer, Integer> durations =
                new HashMap<>();

        double currentRefreshMinutes =
                components.stream()
                        .mapToInt(component ->
                                component.currentMinutes
                        )
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

    private List<RefreshComponent> buildComponents(
            RefreshPlanAiRequest request,
            RefreshPlanAiResponse response
    ) {
        Map<String, AvailableSlotContext> refreshSlots =
                new HashMap<>();
        if (request.availableSlots() != null) {
            for (AvailableSlotContext slot : request.availableSlots()) {
                refreshSlots.put(
                        slot.slotId(),
                        slot
                );
            }
        }

        Map<String, AvailableSlotContext> skinRecoverySlots =
                new HashMap<>();
        if (request.skinRecoveryAvailableSlots() != null) {
            for (AvailableSlotContext slot : request.skinRecoveryAvailableSlots()) {
                skinRecoverySlots.put(
                        slot.slotId(),
                        slot
                );
            }
        }

        List<RefreshComponent> components =
                new ArrayList<>();

        List<RefreshPlanItem> refreshPlan =
                response.refreshPlan() == null
                        ? List.of()
                        : response.refreshPlan();

        for (int i = 0; i < refreshPlan.size(); i++) {
            RefreshPlanItem item =
                    refreshPlan.get(i);
            AvailableSlotContext slot =
                    refreshSlots.get(
                            item.preferredSlotId()
                    );

            if (slot == null) {
                continue;
            }

            components.add(
                    new RefreshComponent(
                            i,
                            item.activityType(),
                            item.preferredSlotId(),
                            item.durationMinutes(),
                            MIN_REFRESH_ACTIVITY_MINUTES,
                            (int) slot.durationMinutes(),
                            false
                    )
            );
        }

        SkinRecoveryPlan skinRecovery =
                response.skinRecovery();

        if (skinRecovery != null
                && skinRecovery.enabled()
                && skinRecovery.preferredSlotId() != null
                && skinRecovery.durationMinutes() != null) {

            AvailableSlotContext slot =
                    skinRecoverySlots.get(
                            skinRecovery.preferredSlotId()
                    );

            if (slot != null) {
                components.add(
                        new RefreshComponent(
                                components.size(),
                                null,
                                skinRecovery.preferredSlotId(),
                                skinRecovery.durationMinutes(),
                                MIN_SKIN_RECOVERY_MINUTES,
                                (int) slot.durationMinutes(),
                                true
                        )
                );
            }
        }

        return components;
    }

    private Integer findAdjustedSkinRecoveryDuration(
            List<RefreshComponent> components,
            Map<Integer, Integer> adjustedDurations,
            Integer fallbackDuration
    ) {
        for (RefreshComponent component : components) {
            if (!component.skinRecovery) {
                continue;
            }

            return adjustedDurations.getOrDefault(
                    component.index,
                    fallbackDuration
            );
        }

        return fallbackDuration;
    }

    private int toAdjustmentMinutes(Integer adjustment) {
        if (adjustment == null) {
            return 0;
        }

        return adjustment * ADJUSTMENT_STEP_MINUTES;
    }

    private record RefreshComponent(
            int index,
            org.example.nura.domain.user.entity.enums.RestActivityType activityType,
            String slotId,
            int currentMinutes,
            int minMinutes,
            int maxMinutes,
            boolean skinRecovery
    ) {
    }
}
