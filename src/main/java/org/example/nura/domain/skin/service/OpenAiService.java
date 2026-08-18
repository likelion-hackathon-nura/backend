package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OpenAiService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    private RestClient openAiClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.openAiClient = RestClient.builder()
                .baseUrl(openAiBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AiAnalysisOutput generateAiAnalysis(
            Integer fatigue,
            Integer tightness,
            Integer redness,
            Integer acneCount,
            Integer rednessScore,
            Integer moistureScore,
            Integer oilinessScore,
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble,
            String scheduleContext
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackAnalysis(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
        }

        try {
            String userPrompt = "다음 사용자 체크인, 피부 분석 정량 데이터, 최근 근무 스케줄 정보를 종합 분석하여 JSON 형태로 응답해 주세요."
                    + "\n\n[최근 근무 스케줄]\n" + scheduleContext
                    + "\n\n[사용자 문진 지표]\n- 피로도(fatigue): " + fatigue
                    + "\n- 피부 당김(tightness): " + tightness
                    + "\n- 붉은기(redness): " + redness
                    + "\n\n[사진 정량 분석 결과]\n- 트러블 개수(acneCount): " + acneCount
                    + "\n- 붉은기 점수(rednessScore): " + rednessScore
                    + "\n- 수분 점수(moistureScore): " + moistureScore
                    + "\n- 유분 점수(oilinessScore): " + oilinessScore
                    + "\n\n[요청 JSON 포맷]"
                    + "\n{"
                    + "\n  \"aiComment\": \"근무 패턴과 피부 상태를 결합한 종합 AI 총평 코멘트 (50자 이내)\","
                    + "\n  \"rednessComment\": \"붉은기 상태 분석 요약문 1문장\","
                    + "\n  \"moistureComment\": \"수분/당김 상태 분석 요약문 1문장\","
                    + "\n  \"troubleComment\": \"트러블/유분 상태 분석 요약문 1문장\","
                    + "\n  \"tags\": [\"태그1\", \"태그2\"]"
                    + "\n}"
                    + "\n* 태그 조건: 근무 맥락(예: 연속 근무 피로 누적, 야간 근무 자극)과 추천 케어 방향(예: 진정과 보습 중심, 속수분 충전)을 포함해 최대 2개의 짧은 태그를 생성하세요.";

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "너는 간호사 등 교대 근무자의 피부 및 시프트 스케줄 분석 전문 AI 헬스케어 어시스턴트이다."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", userPrompt
                            )
                    )
            );

            String responseBody = openAiClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return fallbackAnalysis(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String jsonContent = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode parsedJson = objectMapper.readTree(jsonContent);

            String aiComment = parsedJson.path("aiComment").asText("오늘 피부 상태에 맞춘 맞춤 케어를 추천합니다.");
            String rednessComment = parsedJson.path("rednessComment").asText("붉은기 상태가 관찰됩니다.");
            String moistureComment = parsedJson.path("moistureComment").asText("충분한 수분 공급이 필요합니다.");
            String troubleComment = parsedJson.path("troubleComment").asText("피부 청결 유지가 중요합니다.");

            List<String> tagList = objectMapper.convertValue(
                    parsedJson.path("tags"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            String tags = (tagList != null && !tagList.isEmpty()) ? String.join(",", tagList) : "근무 피로 누적,보습 케어";

            return new AiAnalysisOutput(aiComment, rednessComment, moistureComment, troubleComment, tags);

        } catch (Exception e) {
            log.warn("[OpenAiService] OpenAI 피부 분석 생성 실패: {}", e.getMessage());
            return fallbackAnalysis(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
        }
    }

    private AiAnalysisOutput fallbackAnalysis(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble
    ) {
        String aiComment = "오늘 피부 상태에 맞춘 균형 루틴을 추천합니다.";
        String rednessComment = analyzedRedness == SkinAnalysisLevel.HIGH ? "붉은기가 높아 진정 케어가 필요합니다." : "붉은기 상태가 양호합니다.";
        String moistureComment = analyzedMoisture == SkinAnalysisLevel.LOW ? "수분 지수가 낮아 속건조 케어가 필요합니다." : "수분 지수가 안정적입니다.";
        String troubleComment = analyzedTrouble == SkinAnalysisLevel.HIGH ? "트러블 주의가 필요합니다." : "트러블 우려가 낮은 상태입니다.";
        String tags = "근무 피로 누적,진정과 보습 중심";

        return new AiAnalysisOutput(aiComment, rednessComment, moistureComment, troubleComment, tags);
    }

    public IngredientParseResult parseIngredients(String rawText) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return new IngredientParseResult(rawText, "없음");
        }

        if (rawText == null || rawText.isBlank()) {
            return new IngredientParseResult("성분 정보를 읽을 수 없습니다.", "없음");
        }

        try {
            String prompt = "다음 텍스트는 화장품 뒷면 라벨에서 추출한 OCR 결과입니다.\n"
                    + "이 텍스트에서 '전성분'과 '핵심 성분'을 구분하여 추출해주세요.\n\n"
                    + "1. cosmeticIngredients: 오타가 보정된 전체 전성분 (쉼표로 구분)\n"
                    + "2. coreIngredients: 정제수, 글리세린, 부틸렌글라이콜 등 기본 베이스를 제외하고 피부 개선 효과가 뛰어난 주요 핵심 성분 1~4개 (쉼표로 구분)\n\n"
                    + "응답은 반드시 아래 JSON 포맷으로만 답변하세요:\n"
                    + "{\n"
                    + "  \"cosmeticIngredients\": \"...\",\n"
                    + "  \"coreIngredients\": \"...\"\n"
                    + "}\n\n"
                    + "텍스트:\n" + rawText;

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", "너는 화장품 전성분 분석 파서(Parser)이다."),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String responseBody = openAiClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            String jsonContent = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode parsedJson = objectMapper.readTree(jsonContent);

            return new IngredientParseResult(
                    parsedJson.path("cosmeticIngredients").asText(rawText),
                    parsedJson.path("coreIngredients").asText("없음")
            );

        } catch (Exception e) {
            log.warn("[OpenAiService] 성분 파싱 실패: {}", e.getMessage());
            return new IngredientParseResult(rawText, "없음");
        }
    }

    @Getter
    @AllArgsConstructor
    public static class AiAnalysisOutput {
        private String aiComment;
        private String rednessComment;
        private String moistureComment;
        private String troubleComment;
        private String tags;
    }

    public record IngredientParseResult(
            String cosmeticIngredients,
            String coreIngredients
    ) {}
}