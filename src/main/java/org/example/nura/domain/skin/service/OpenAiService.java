package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

    public String generateAiComment(
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
            SkinAnalysisLevel analyzedTrouble
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackComment(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
        }

        try {
            String userPrompt = "사용자 체크인 정보와 정량 분석 결과를 바탕으로 한국어 1문장 피부 피드백을 작성해주세요."
                    + "\n- fatigue: " + fatigue
                    + "\n- tightness: " + tightness
                    + "\n- redness: " + redness
                    + "\n- acne_count: " + acneCount
                    + "\n- redness_score: " + rednessScore
                    + "\n- moisture_score: " + moistureScore
                    + "\n- oiliness_score: " + oilinessScore;

            Map<String, Object> payload = Map.of(
                    "model", openAiModel,
                    "temperature", 0.4,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "너는 피부 진단 코멘트를 작성하는 어시스턴트다. 40자 이내로 간결하게 작성한다."
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
                return fallbackComment(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (contentNode.isMissingNode()) {
                return fallbackComment(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
            }

            String comment = contentNode.asText().trim();
            return comment.isBlank()
                    ? fallbackComment(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble)
                    : comment;

        } catch (Exception e) {
            log.warn("[OpenAiService] OpenAI API 코멘트 생성 호출 실패: {}", e.getMessage());
            return fallbackComment(analyzedRedness, analyzedMoisture, analyzedOiliness, analyzedTrouble);
        }
    }

    private String fallbackComment(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble
    ) {
        if (analyzedTrouble == SkinAnalysisLevel.HIGH || analyzedRedness == SkinAnalysisLevel.HIGH) {
            return "붉은기/트러블이 높아 진정 위주 케어를 권장합니다.";
        }

        if (analyzedMoisture == SkinAnalysisLevel.LOW) {
            return "수분 지표가 낮아 보습 중심 루틴을 권장합니다.";
        }

        if (analyzedOiliness == SkinAnalysisLevel.HIGH) {
            return "유분 지표가 높아 가벼운 수분 케어를 권장합니다.";
        }

        return "오늘 피부 상태에 맞는 균형 루틴을 권장합니다.";
    }
}