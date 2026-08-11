package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class SkinAnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openAiBaseUrl;

    @Value("${ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String openAiModel;

    public AnalysisResult analyze(
            MultipartFile photo,
            Integer fatigue,
            Integer tightness,
            Integer redness
    ) {
        if (photo == null || photo.isEmpty()) {
            return new AnalysisResult(
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    "사진이 없어 문진 결과 기준으로 루틴을 추천합니다."
            );
        }

        QuantitativeMetrics metrics = requestQuantitativeMetrics(photo);

        SkinAnalysisLevel analyzedRedness = mapScoreToLevel(metrics.rednessScore());
        SkinAnalysisLevel analyzedMoisture = mapScoreToLevel(metrics.moistureScore());
        SkinAnalysisLevel analyzedOiliness = mapScoreToLevel(metrics.oilinessScore());
        SkinAnalysisLevel analyzedTrouble = mapAcneToLevel(metrics.acneCount());

        String aiComment = requestAiComment(
                fatigue,
                tightness,
                redness,
                metrics,
                analyzedRedness,
                analyzedMoisture,
                analyzedOiliness,
                analyzedTrouble
        );

        return new AnalysisResult(
                analyzedRedness,
                analyzedMoisture,
                analyzedOiliness,
                analyzedTrouble,
                aiComment
        );
    }

    private QuantitativeMetrics requestQuantitativeMetrics(MultipartFile photo) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("photo", asByteArrayResource(photo));

            String responseBody = RestClient.builder()
                    .baseUrl(fastApiBaseUrl)
                    .build()
                    .post()
                    .uri("/analyze")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return QuantitativeMetrics.empty();
            }

            JsonNode root = objectMapper.readTree(responseBody);

            return new QuantitativeMetrics(
                    readNullableInt(root, "acne_count"),
                    readNullableInt(root, "redness_score"),
                    readNullableInt(root, "moisture_score"),
                    readNullableInt(root, "oiliness_score")
            );
        } catch (Exception e) {
            return QuantitativeMetrics.empty();
        }
    }

    private String requestAiComment(
            Integer fatigue,
            Integer tightness,
            Integer redness,
            QuantitativeMetrics metrics,
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallbackComment(
                    analyzedRedness,
                    analyzedMoisture,
                    analyzedOiliness,
                    analyzedTrouble
            );
        }

        try {
            String userPrompt = "사용자 체크인 정보와 정량 분석 결과를 바탕으로 한국어 1문장 피부 피드백을 작성해주세요."
                    + "\n- fatigue: " + fatigue
                    + "\n- tightness: " + tightness
                    + "\n- redness: " + redness
                    + "\n- acne_count: " + metrics.acneCount()
                    + "\n- redness_score: " + metrics.rednessScore()
                    + "\n- moisture_score: " + metrics.moistureScore()
                    + "\n- oiliness_score: " + metrics.oilinessScore();

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

            String responseBody = RestClient.builder()
                    .baseUrl(openAiBaseUrl)
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return fallbackComment(
                        analyzedRedness,
                        analyzedMoisture,
                        analyzedOiliness,
                        analyzedTrouble
                );
            }

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices")
                    .path(0)
                    .path("message")
                    .path("content");

            if (contentNode.isMissingNode()) {
                return fallbackComment(
                        analyzedRedness,
                        analyzedMoisture,
                        analyzedOiliness,
                        analyzedTrouble
                );
            }

            String comment = contentNode.asText().trim();
            if (comment.isBlank()) {
                return fallbackComment(
                        analyzedRedness,
                        analyzedMoisture,
                        analyzedOiliness,
                        analyzedTrouble
                );
            }

            return comment;
        } catch (Exception e) {
            return fallbackComment(
                    analyzedRedness,
                    analyzedMoisture,
                    analyzedOiliness,
                    analyzedTrouble
            );
        }
    }

    private ByteArrayResource asByteArrayResource(MultipartFile photo) throws IOException {
        return new ByteArrayResource(photo.getBytes()) {
            @Override
            public String getFilename() {
                return photo.getOriginalFilename() == null
                        ? "skin-photo.jpg"
                        : photo.getOriginalFilename();
            }
        };
    }

    private Integer readNullableInt(
            JsonNode root,
            String key
    ) {
        JsonNode node = root.path(key);
        return node.isNumber() ? node.asInt() : null;
    }

    private SkinAnalysisLevel mapScoreToLevel(Integer score) {
        if (score == null) {
            return SkinAnalysisLevel.UNKNOWN;
        }

        if (score <= 33) {
            return SkinAnalysisLevel.LOW;
        }
        if (score <= 66) {
            return SkinAnalysisLevel.MEDIUM;
        }
        return SkinAnalysisLevel.HIGH;
    }

    private SkinAnalysisLevel mapAcneToLevel(Integer acneCount) {
        if (acneCount == null) {
            return SkinAnalysisLevel.UNKNOWN;
        }

        if (acneCount == 0) {
            return SkinAnalysisLevel.LOW;
        }
        if (acneCount <= 3) {
            return SkinAnalysisLevel.MEDIUM;
        }
        return SkinAnalysisLevel.HIGH;
    }

    private String fallbackComment(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble
    ) {
        if (analyzedTrouble == SkinAnalysisLevel.HIGH
                || analyzedRedness == SkinAnalysisLevel.HIGH) {
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

    public record AnalysisResult(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble,
            String aiComment
    ) {
    }

    private record QuantitativeMetrics(
            Integer acneCount,
            Integer rednessScore,
            Integer moistureScore,
            Integer oilinessScore
    ) {
        private static QuantitativeMetrics empty() {
            return new QuantitativeMetrics(null, null, null, null);
        }
    }
}


