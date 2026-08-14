package org.example.nura.domain.schedule.dto.plan;

import java.util.List;

public record RefreshAllocationResult(
        List<RefreshActivityAllocation> activities,
        SkinRecoveryAllocation skinRecovery
) {
}
