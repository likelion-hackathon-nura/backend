package org.example.nura.domain.skin.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.nura.domain.cosmetics.entity.RegisteredCosmetic;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.RoutineStep;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.example.nura.domain.skin.entity.enums.SkinCareType;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Getter
@Builder
public class SkinMainTodayResponse {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @JsonProperty("isCheckedIn")
    private boolean isCheckedIn;

    @JsonProperty("isRoutineCompleted")
    private boolean isRoutineCompleted;

    private int streakDays;
    private List<WeeklyRecordDto> weeklyRecords;
    private CheckinSummaryDto checkinSummary;
    private RoutineSummaryDto routineSummary;

    @Getter
    @Builder
    public static class WeeklyRecordDto {
        private String dayOfWeek;
        private LocalDate date;
        private String status; // "NONE", "CHECKED_IN", "COMPLETED"
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
        private List<String> tags;
        private List<AnalysisDetailDto> analysisDetails;

        public static CheckinSummaryDto from(Checkin checkin) {
            List<String> parsedTags = (checkin.getTags() != null && !checkin.getTags().isBlank())
                    ? Arrays.stream(checkin.getTags().split(",")).map(String::trim).toList()
                    : List.of("근무 피로 누적", "진정과 보습 중심");

            List<AnalysisDetailDto> details = List.of(
                    new AnalysisDetailDto("붉은기", checkin.getAnalyzedRedness().name(),
                            checkin.getRednessComment() != null ? checkin.getRednessComment() : "붉은기 상태가 양호합니다."),
                    new AnalysisDetailDto("수분 부족", checkin.getAnalyzedMoisture().name(),
                            checkin.getMoistureComment() != null ? checkin.getMoistureComment() : "충분한 수분 케어가 권장됩니다."),
                    new AnalysisDetailDto("트러블 징후", checkin.getAnalyzedTrouble().name(),
                            checkin.getTroubleComment() != null ? checkin.getTroubleComment() : "트러블 위험도가 낮습니다.")
            );

            return CheckinSummaryDto.builder()
                    .fatigue(checkin.getFatigue())
                    .tightness(checkin.getTightness())
                    .redness(checkin.getRedness())
                    .analyzedRedness(checkin.getAnalyzedRedness())
                    .analyzedMoisture(checkin.getAnlyzedMoisture())
                    .analyzedOiliness(checkin.getAnalyzedOiliness())
                    .analyzedTrouble(checkin.getAnalyzedTrouble())
                    .aiComment(checkin.getAiComment())
                    .tags(parsedTags)
                    .analysisDetails(details)
                    .build();
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AnalysisDetailDto {
        private String title;
        private String level;
        private String description;
    }

    @Getter
    @Builder
    public static class RoutineSummaryDto {
        private Long routineId;
        private String recoveryLevel;
        private boolean completed;
        private int totalSteps;
        private List<RoutineStepDto> steps;

        public static RoutineSummaryDto of(SkinRoutine routine, List<RoutineStep> steps) {
            List<RoutineStepDto> stepDtos = steps.stream()
                    .map(RoutineStepDto::from)
                    .toList();

            return RoutineSummaryDto.builder()
                    .routineId(routine.getId())
                    .recoveryLevel(routine.getRecoveryLevel().name())
                    .completed(routine.isCompleted())
                    .totalSteps(steps.size())
                    .steps(stepDtos)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoutineStepDto {
        private int stepOrder;
        private String careType;
        private String careTypeKr;
        private boolean hasCosmetic;

        private String cosmeticName;
        private String cosmeticBrand;
        private String cosmeticImageUrl;
        private List<String> cosmeticFeatures;

        private List<String> recommendedIngredients;
        private String recommendedIngredientDescription;

        public static RoutineStepDto from(RoutineStep step) {
            RegisteredCosmetic cosmetic = step.getRegisteredCosmetic();
            boolean hasCosmetic = (cosmetic != null);
            SkinCareType careType = step.getCareType();

            if (hasCosmetic) {
                String coreIng = (cosmetic.getCoreIngredients() != null && !cosmetic.getCoreIngredients().isBlank())
                        ? cosmetic.getCoreIngredients()
                        : "진정 및 보습 성분";

                List<String> features = List.of(
                        getCareTypeKr(careType) + " 케어 지원",
                        coreIng + " 함유"
                );

                return RoutineStepDto.builder()
                        .stepOrder(step.getStepOrder())
                        .careType(careType != null ? careType.name() : null)
                        .careTypeKr(getCareTypeKr(careType))
                        .hasCosmetic(true)
                        .cosmeticName(cosmetic.getCosmeticName())
                        .cosmeticBrand(cosmetic.getCosmeticBrand())
                        .cosmeticImageUrl(cosmetic.getCosmeticUrl())
                        .cosmeticFeatures(features)
                        .recommendedIngredients(null)
                        .recommendedIngredientDescription(null)
                        .build();
            } else {
                List<String> ingredients = parseIngredientsJson(step.getRecommendedIngredients());
                String ingredientDesc = getRecommendedIngredientDescription(careType);

                return RoutineStepDto.builder()
                        .stepOrder(step.getStepOrder())
                        .careType(careType != null ? careType.name() : null)
                        .careTypeKr(getCareTypeKr(careType))
                        .hasCosmetic(false)
                        .cosmeticName(getCareTypeKr(careType) + " 추천 성분 케어")
                        .cosmeticBrand(null)
                        .cosmeticImageUrl(null)
                        .cosmeticFeatures(null)
                        .recommendedIngredients(ingredients)
                        .recommendedIngredientDescription(ingredientDesc)
                        .build();
            }
        }

        private static List<String> parseIngredientsJson(String json) {
            if (json == null || json.isBlank()) return List.of("기본 보습 성분");
            try {
                return objectMapper.readValue(json, new TypeReference<>() {});
            } catch (Exception e) {
                return List.of(json.replace("[", "").replace("]", "").replace("\"", "").split(","));
            }
        }

        private static String getCareTypeKr(SkinCareType careType) {
            if (careType == null) return "케어";
            return switch (careType) {
                case SOOTHING -> "진정 케어";
                case HYDRATION -> "보습 케어";
                case MOISTURIZING -> "영양 케어";
                case BARRIER_CARE -> "장벽 케어";
                case OIL_CONTROL -> "피지 조절";
                case TROUBLE_CARE -> "트러블 케어";
            };
        }

        private static String getRecommendedIngredientDescription(SkinCareType careType) {
            if (careType == null) return "피부 상태 개선에 도움을 주는 추천 성분이에요.";
            return switch (careType) {
                case SOOTHING -> "자극을 완화하고 피부를 편안하게 진정시키는 추천 성분이에요.";
                case HYDRATION -> "피부 속 수분을 유지하고 당김을 완화하는 추천 성분이에요.";
                case MOISTURIZING -> "영양을 공급하고 피부 장벽을 단단하게 메워주는 추천 성분이에요.";
                case BARRIER_CARE -> "손상된 피부 장벽을 회복하고 보호막을 형성해주는 성분이에요.";
                case OIL_CONTROL -> "과도한 피지를 조절하고 유수분 밸런스를 잡아주는 성분이에요.";
                case TROUBLE_CARE -> "트러블 부위를 빠르게 진정시키고 케어해주는 추천 성분이에요.";
            };
        }
    }
}