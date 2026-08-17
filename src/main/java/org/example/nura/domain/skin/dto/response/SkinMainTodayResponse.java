package org.example.nura.domain.skin.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class SkinMainTodayResponse {

    private boolean isCheckedIn;
    private int streakDays;
    private List<WeeklyRecordDto> weeklyRecords;
    private CheckinSummaryDto checkinSummary;
    private RoutineSummaryDto routineSummary;

    @Getter
    @Builder
    public static class WeeklyRecordDto {
        private String dayOfWeek; // "MON", "TUE" ...
        private LocalDate date;
        private String status;    // "COMPLETED", "NONE"
    }

    @Getter
    @Builder
    public static class CheckinSummaryDto {
        private Integer fatigue;
        private Integer tightness;
        private Integer redness;
        private String photoUrl;
        private SkinAnalysisLevel analyzedRedness;
        private SkinAnalysisLevel analyzedMoisture;
        private SkinAnalysisLevel analyzedOiliness;
        private SkinAnalysisLevel analyzedTrouble;
        private String aiComment;

        public static CheckinSummaryDto from(Checkin checkin) {
            return CheckinSummaryDto.builder()
                    .fatigue(checkin.getFatigue())
                    .tightness(checkin.getTightness())
                    .redness(checkin.getRedness())
                    .photoUrl(checkin.getPhotoUrl())
                    .analyzedRedness(checkin.getAnalyzedRedness())
                    .analyzedMoisture(checkin.getAnalyzedMoisture())
                    .analyzedOiliness(checkin.getAnalyzedOiliness())
                    .analyzedTrouble(checkin.getAnalyzedTrouble())
                    .aiComment(checkin.getAiComment())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoutineSummaryDto {
        private Long routineId;
        private String recoveryLevel;
        private boolean completed;

        public static RoutineSummaryDto from(SkinRoutine routine) {
            return RoutineSummaryDto.builder()
                    .routineId(routine.getId())
                    .recoveryLevel(routine.getRecoveryLevel().name())
                    .completed(routine.isCompleted())
                    .build();
        }
    }
}