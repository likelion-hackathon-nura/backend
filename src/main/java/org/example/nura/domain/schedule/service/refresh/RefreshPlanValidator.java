// ai 응답 검증
package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanItem;
import org.example.nura.domain.schedule.dto.ai.SkinRecoveryPlan;
import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RefreshPlanValidator {

    private static final int MIN_SKIN_RECOVERY_MINUTES = 15;
    private static final int MAX_SKIN_RECOVERY_MINUTES = 30;

    public void validate(
            RefreshPlanAiRequest request,
            RefreshPlanAiResponse response
    ) {
        if (response == null) {
            throw invalidResponse();
        }

        // 일반 회복 활동용 슬롯
        Map<String, AvailableSlotContext> availableSlotMap =
                request.availableSlots()
                        .stream()
                        .collect(Collectors.toMap(
                                AvailableSlotContext::slotId,
                                slot -> slot
                        ));

        // 피부 회복 전용 슬롯
        Map<String, AvailableSlotContext> skinRecoverySlotMap =
                request.skinRecoveryAvailableSlots()
                        .stream()
                        .collect(Collectors.toMap(
                                AvailableSlotContext::slotId,
                                slot -> slot
                        ));

        Set<RestActivityType> allowedActivities =
                Set.copyOf(request.restActivities());

        // 일반 회복 활동 슬롯 사용량
        Map<String, Integer> refreshUsedMinutesBySlot =
                new HashMap<>();

        // 피부 회복 슬롯 사용량
        Map<String, Integer> skinUsedMinutesBySlot =
                new HashMap<>();

        validateRefreshActivities(
                response,
                allowedActivities,
                availableSlotMap,
                refreshUsedMinutesBySlot
        );

        validateSkinRecovery(
                response.skinRecovery(),
                skinRecoverySlotMap,
                skinUsedMinutesBySlot
        );
    }

    private void validateRefreshActivities(
            RefreshPlanAiResponse response,
            Set<RestActivityType> allowedActivities,
            Map<String, AvailableSlotContext> slotMap,
            Map<String, Integer> usedMinutesBySlot
    ) {
        if (response.refreshPlan() == null) {
            return;
        }

        for (RefreshPlanItem item : response.refreshPlan()) {

            if (item == null
                    || item.activityType() == null
                    || item.preferredSlotId() == null) {
                throw invalidResponse();
            }

            // 온보딩에서 선택한 활동인지 확인
            if (!allowedActivities.contains(
                    item.activityType()
            )) {
                throw new BaseException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "AI가 선택하지 않은 휴식 활동을 반환했습니다."
                );
            }

            if (item.durationMinutes() <= 0) {
                throw invalidResponse();
            }

            AvailableSlotContext slot =
                    slotMap.get(
                            item.preferredSlotId()
                    );

            if (slot == null) {
                throw new BaseException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "AI가 존재하지 않는 시간대를 반환했습니다."
                );
            }

            addSlotUsage(
                    slot,
                    item.durationMinutes(),
                    usedMinutesBySlot
            );
        }
    }

    private void validateSkinRecovery(
            SkinRecoveryPlan skinRecovery,
            Map<String, AvailableSlotContext> slotMap,
            Map<String, Integer> usedMinutesBySlot
    ) {
        if (skinRecovery == null
                || !skinRecovery.enabled()) {
            return;
        }

        if (skinRecovery.preferredSlotId() == null) {
            throw invalidResponse();
        }

        if (skinRecovery.durationMinutes()
                < MIN_SKIN_RECOVERY_MINUTES
                || skinRecovery.durationMinutes()
                > MAX_SKIN_RECOVERY_MINUTES) {

            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "AI가 피부 회복 시간을 허용 범위 밖으로 반환했습니다."
            );
        }

        AvailableSlotContext slot =
                slotMap.get(
                        skinRecovery.preferredSlotId()
                );

        if (slot == null) {
            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "AI가 존재하지 않는 피부 회복 시간대를 반환했습니다."
            );
        }

        addSlotUsage(
                slot,
                skinRecovery.durationMinutes(),
                usedMinutesBySlot
        );
    }

    private void addSlotUsage(
            AvailableSlotContext slot,
            int additionalMinutes,
            Map<String, Integer> usedMinutesBySlot
    ) {
        int currentMinutes =
                usedMinutesBySlot.getOrDefault(
                        slot.slotId(),
                        0
                );

        int totalMinutes =
                currentMinutes
                        + additionalMinutes;

        if (totalMinutes > slot.durationMinutes()) {
            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "AI가 시간대보다 긴 회복 계획을 반환했습니다."
            );
        }

        usedMinutesBySlot.put(
                slot.slotId(),
                totalMinutes
        );
    }

    private BaseException invalidResponse() {
        return new BaseException(
                ErrorCode.EXTERNAL_API_ERROR,
                "AI 회복 추천 결과가 올바르지 않습니다."
        );
    }
}
