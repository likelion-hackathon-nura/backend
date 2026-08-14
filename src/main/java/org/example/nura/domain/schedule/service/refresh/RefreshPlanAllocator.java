// ai가 고른 슬롯아이디 + 시간을 실제 시간으로 배치
package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanItem;
import org.example.nura.domain.schedule.dto.ai.SkinRecoveryPlan;
import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.dto.plan.RefreshActivityAllocation;
import org.example.nura.domain.schedule.dto.plan.RefreshAllocationResult;
import org.example.nura.domain.schedule.dto.plan.SkinRecoveryAllocation;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RefreshPlanAllocator {

    public RefreshAllocationResult allocate(
            RefreshPlanAiRequest request,
            RefreshPlanAiResponse response
    ) {
        // 일반 회복 활동용 슬롯
        Map<String, AvailableSlotContext> availableSlotMap =
                request.availableSlots()
                        .stream()
                        .collect(Collectors.toMap(
                                AvailableSlotContext::slotId,
                                Function.identity()
                        ));

        // 피부 회복 전용 슬롯
        Map<String, AvailableSlotContext> skinRecoverySlotMap =
                request.skinRecoveryAvailableSlots()
                        .stream()
                        .collect(Collectors.toMap(
                                AvailableSlotContext::slotId,
                                Function.identity()
                        ));

        // 슬롯별 다음 배치 시작 시간
        Map<String, LocalDateTime> cursorMap =
                new HashMap<>();

        List<RefreshActivityAllocation> activities =
                new ArrayList<>();

        // 일반 RestActivity 배치
        if (response.refreshPlan() != null) {

            for (RefreshPlanItem item : response.refreshPlan()) {

                AvailableSlotContext slot =
                        availableSlotMap.get(
                                item.preferredSlotId()
                        );

                TimeInterval interval =
                        allocateFromSlot(
                                slot,
                                item.durationMinutes(),
                                cursorMap
                        );

                activities.add(
                        new RefreshActivityAllocation(
                                item.activityType(),
                                interval
                        )
                );
            }
        }

        SkinRecoveryAllocation skinRecoveryAllocation =
                null;

        SkinRecoveryPlan skinRecovery =
                response.skinRecovery();

        // 피부 회복 배치
        if (skinRecovery != null
                && skinRecovery.enabled()) {

            AvailableSlotContext slot =
                    skinRecoverySlotMap.get(
                            skinRecovery.preferredSlotId()
                    );

            TimeInterval interval =
                    allocateFromSlot(
                            slot,
                            skinRecovery.durationMinutes(),
                            cursorMap
                    );

            skinRecoveryAllocation =
                    new SkinRecoveryAllocation(
                            interval
                    );
        }

        return new RefreshAllocationResult(
                activities,
                skinRecoveryAllocation
        );
    }

    private TimeInterval allocateFromSlot(
            AvailableSlotContext slot,
            int durationMinutes,
            Map<String, LocalDateTime> cursorMap
    ) {
        if (slot == null) {
            throw new IllegalStateException(
                    "회복 활동을 배치할 수 있는 시간대가 없습니다."
            );
        }

        LocalDateTime cursor =
                cursorMap.get(
                        slot.slotId()
                );

        // 슬롯 시작 시간보다 앞에 배치되지 않도록 보정
        LocalDateTime startAt;

        if (cursor == null
                || cursor.isBefore(slot.startAt())) {

            startAt =
                    slot.startAt();

        } else {
            startAt =
                    cursor;
        }

        LocalDateTime endAt =
                startAt.plusMinutes(
                        durationMinutes
                );

        // 실제 슬롯 범위를 넘으면 차단
        if (endAt.isAfter(slot.endAt())) {
            throw new IllegalStateException(
                    "회복 활동이 사용 가능한 시간대를 초과했습니다."
            );
        }

        cursorMap.put(
                slot.slotId(),
                endAt
        );

        return new TimeInterval(
                startAt,
                endAt
        );
    }
}