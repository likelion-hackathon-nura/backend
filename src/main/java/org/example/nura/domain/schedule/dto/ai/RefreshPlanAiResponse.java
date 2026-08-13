package org.example.nura.domain.schedule.dto.ai;

import java.util.List;

public record RefreshPlanAiResponse(
        List<RefreshPlanItem> refreshPlan,
        SkinRecoveryPlan skinRecovery
) {

    public static RefreshPlanAiResponse fallback() {
        return new RefreshPlanAiResponse(
                List.of(),
                new SkinRecoveryPlan(
                        false,
                        null,
                        null
                )
        );
    }
}
