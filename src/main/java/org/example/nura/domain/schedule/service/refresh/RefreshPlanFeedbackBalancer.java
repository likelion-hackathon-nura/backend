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

    private static final int MIN_REFRESH_ACTIVITY_MINUTES = 5;
    private static final int ADJUSTMENT_STEP_MINUTES = 5;
    private static final int MAX_SHIFT_MINUTES = 15;

    public RefreshPlanAiResponse balance(
            RefreshPlanAiRequest request,
            RefreshPlanAiResponse response,
            int availablePoolMinutes
    ) {
        if (response == null || response.refreshPlan() == null) {
            return response;
        }

        int balanceScore =
                request.balanceScore() == null
                        ? 0
                        : request.balanceScore();

        if (balanceScore == 0) {
            return response;
        }

        List<RefreshPlanItem> plan =
                response.refreshPlan();
        if (plan.isEmpty() || availablePoolMinutes <= 0) {
            return response;
        }

        Map<String, Integer> slotCapacity =
                toSlotCapacity(request.availableSlots());
        if (slotCapacity.isEmpty()) {
            return response;
        }

        reserveSkinRecoveryCapacity(
                response.skinRecovery(),
                slotCapacity
        );

        List<ItemState> states =
                buildStates(
                        plan,
                        slotCapacity
                );
        if (states.isEmpty()) {
            return response;
        }

        int shiftMinutes =
                Math.min(
                        MAX_SHIFT_MINUTES,
                        Math.abs(balanceScore) * ADJUSTMENT_STEP_MINUTES
                );

        boolean refreshPreferred =
                balanceScore > 0;

        int appliedShift =
                refreshPreferred
                        ? increaseAsMuchAsPossible(states, slotCapacity, shiftMinutes)
                        : decreaseAsMuchAsPossible(states, shiftMinutes);

        if (appliedShift <= 0) {
            return response;
        }

        List<RefreshPlanItem> adjustedItems =
                new ArrayList<>(plan.size());
        Map<Integer, ItemState> stateByIndex =
                new HashMap<>();
        for (ItemState state : states) {
            stateByIndex.put(
                    state.index,
                    state
            );
        }

        for (int i = 0; i < plan.size(); i++) {
            ItemState state =
                    stateByIndex.get(i);

            if (state == null) {
                adjustedItems.add(plan.get(i));
                continue;
            }

            adjustedItems.add(
                    new RefreshPlanItem(
                            state.activityType,
                            state.currentMinutes,
                            state.slotId
                    )
            );
        }

        return new RefreshPlanAiResponse(
                adjustedItems,
                response.skinRecovery()
        );
    }

    private Map<String, Integer> toSlotCapacity(
            List<AvailableSlotContext> availableSlots
    ) {
        Map<String, Integer> slotCapacity =
                new HashMap<>();

        if (availableSlots == null) {
            return slotCapacity;
        }

        for (AvailableSlotContext slot : availableSlots) {
            slotCapacity.put(
                    slot.slotId(),
                    (int) slot.durationMinutes()
            );
        }

        return slotCapacity;
    }

    private void reserveSkinRecoveryCapacity(
            SkinRecoveryPlan skinRecovery,
            Map<String, Integer> slotCapacity
    ) {
        if (skinRecovery == null
                || !skinRecovery.enabled()
                || skinRecovery.preferredSlotId() == null
                || skinRecovery.durationMinutes() == null) {
            return;
        }

        Integer capacity =
                slotCapacity.get(
                        skinRecovery.preferredSlotId()
                );
        if (capacity == null) {
            return;
        }

        slotCapacity.put(
                skinRecovery.preferredSlotId(),
                Math.max(0, capacity - skinRecovery.durationMinutes())
        );
    }

    private List<ItemState> buildStates(
            List<RefreshPlanItem> plan,
            Map<String, Integer> slotCapacity
    ) {
        List<ItemState> states =
                new ArrayList<>();

        for (int i = 0; i < plan.size(); i++) {
            RefreshPlanItem item = plan.get(i);
            if (item == null
                    || item.preferredSlotId() == null
                    || item.activityType() == null) {
                continue;
            }

            Integer capacity =
                    slotCapacity.get(item.preferredSlotId());
            if (capacity == null) {
                continue;
            }

            states.add(
                    new ItemState(
                            i,
                            item.activityType(),
                            item.preferredSlotId(),
                            item.durationMinutes()
                    )
            );
        }

        return states;
    }

    private int increaseAsMuchAsPossible(
            List<ItemState> states,
            Map<String, Integer> slotCapacity,
            int requestedShift
    ) {
        int applied = 0;

        Map<String, Integer> slotUsed =
                new HashMap<>();
        for (ItemState state : states) {
            slotUsed.merge(
                    state.slotId,
                    state.currentMinutes,
                    Integer::sum
            );
        }

        List<ItemState> order =
                states.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        (ItemState s) -> s.currentMinutes
                                ).reversed()
                        )
                        .toList();

        while (applied < requestedShift) {
            boolean changed = false;

            for (ItemState state : order) {
                int used =
                        slotUsed.getOrDefault(
                                state.slotId,
                                0
                        );
                int capacity =
                        slotCapacity.getOrDefault(
                                state.slotId,
                                0
                        );
                if (used >= capacity) {
                    continue;
                }

                state.currentMinutes += 1;
                slotUsed.put(
                        state.slotId,
                        used + 1
                );
                applied++;
                changed = true;

                if (applied >= requestedShift) {
                    break;
                }
            }

            if (!changed) {
                break;
            }
        }

        return applied;
    }

    private int decreaseAsMuchAsPossible(
            List<ItemState> states,
            int requestedShift
    ) {
        int applied = 0;

        List<ItemState> order =
                states.stream()
                        .sorted(
                                Comparator.comparingInt(
                                        (ItemState s) -> s.currentMinutes
                                )
                        )
                        .toList();

        while (applied < requestedShift) {
            boolean changed = false;

            for (ItemState state : order) {
                if (state.currentMinutes <= MIN_REFRESH_ACTIVITY_MINUTES) {
                    continue;
                }

                state.currentMinutes -= 1;
                applied++;
                changed = true;

                if (applied >= requestedShift) {
                    break;
                }
            }

            if (!changed) {
                break;
            }
        }

        return applied;
    }

    private static class ItemState {
        private final int index;
        private final org.example.nura.domain.user.entity.enums.RestActivityType activityType;
        private final String slotId;
        private int currentMinutes;

        private ItemState(
                int index,
                org.example.nura.domain.user.entity.enums.RestActivityType activityType,
                String slotId,
                int currentMinutes
        ) {
            this.index = index;
            this.activityType = activityType;
            this.slotId = slotId;
            this.currentMinutes = currentMinutes;
        }
    }
}
