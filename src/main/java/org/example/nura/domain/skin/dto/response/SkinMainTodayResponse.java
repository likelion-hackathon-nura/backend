package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("is_checked_in")
    private boolean isCheckedIn; // 오늘 퇴근 체크인 완료 여부

    @JsonProperty("is_routine_completed")
    private boolean isRoutineCompleted; // 오늘 3분 회복 모드 완료 여부

    private int streakDays;
    private List<WeeklyRecordDto> weeklyRecords;
    private CheckinSummaryDto checkinSummary;
    private RoutineSummaryDto routineSummary;

    @Getter
    @Builder
    public static class WeeklyRecordDto {
        private String dayOfWeek;
        private LocalDate date;
        private String status;
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