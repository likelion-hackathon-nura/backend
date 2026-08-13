package org.example.nura.domain.schedule.dto.ai;

import java.util.List;

public record RefreshPlanAiResponse(
        List<RefreshPlanItem> refreshPlan,
        SkinRecoveryPlan skinRecovery
) {
}
