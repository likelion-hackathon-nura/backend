package org.example.nura.domain.home.service;

import org.example.nura.domain.home.dto.response.HomeBadgeResponse;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HomeBadgeGenerator {

    private static final int HIGH_FATIGUE_THRESHOLD = 4;
    private static final int HIGH_TIGHTNESS_THRESHOLD = 3;

    public List<HomeBadgeResponse> generate(
            DailyPlanContext context
    ) {
        List<HomeBadgeResponse> badges =
                new ArrayList<>();

        addNightShiftBadge(
                badges,
                context
        );

        addFatigueBadge(
                badges,
                context
        );

        addSkinTightnessBadge(
                badges,
                context
        );

        addRecoveryRoutineBadge(
                badges,
                context
        );

        return badges;
    }

    // 연속 나이트 근무
    private void addNightShiftBadge(
            List<HomeBadgeResponse> badges,
            DailyPlanContext context
    ) {
        int consecutiveNightDays =
                context.consecutiveNightShiftDays();

        if (consecutiveNightDays < 2) {
            return;
        }

        badges.add(
                new HomeBadgeResponse(
                        "MOON",
                        consecutiveNightDays
                                + "일 연속 나이트 근무"
                )
        );
    }

    // 전날 피로도
    private void addFatigueBadge(
            List<HomeBadgeResponse> badges,
            DailyPlanContext context
    ) {
        if (context.previousCheckin() == null) {
            return;
        }

        Integer fatigue =
                context.previousCheckin()
                        .fatigue();

        if (fatigue == null
                || fatigue < HIGH_FATIGUE_THRESHOLD) {
            return;
        }

        badges.add(
                new HomeBadgeResponse(
                        "FATIGUE",
                        "피로도 "
                                + fatigue
                                + "점 기록"
                )
        );
    }

    // 전날 피부 당김
    private void addSkinTightnessBadge(
            List<HomeBadgeResponse> badges,
            DailyPlanContext context
    ) {
        if (context.previousCheckin() == null) {
            return;
        }

        Integer tightness =
                context.previousCheckin()
                        .tightness();

        if (tightness == null
                || tightness < HIGH_TIGHTNESS_THRESHOLD) {
            return;
        }

        badges.add(
                new HomeBadgeResponse(
                        "SKIN",
                        "피부 당김"
                )
        );
    }

    // 전날 회복 루틴 미완료
    private void addRecoveryRoutineBadge(
            List<HomeBadgeResponse> badges,
            DailyPlanContext context
    ) {
        if (!Boolean.FALSE.equals(
                context.previousRecoveryRoutineCompleted()
        )) {
            return;
        }

        badges.add(
                new HomeBadgeResponse(
                        "RECOVERY",
                        "어제 회복 루틴 미완료"
                )
        );
    }
}
