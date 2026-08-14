package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.skin.entity.enums.SkinAnalysisLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisService {

    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.fastapi.base-url:http://localhost:8000}")
    private String fastApiBaseUrl;

    private RestClient fastApiClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.fastApiClient = RestClient.builder()
                .baseUrl(fastApiBaseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("ngrok-skip-browser-warning", "69420")
                .defaultHeader("User-Agent", "SpringBoot-SkinAnalysis-App")
                .build();
    }

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

        String aiComment = openAiService.generateAiComment(
                fatigue,
                tightness,
                redness,
                metrics.acneCount(),
                metrics.rednessScore(),
                metrics.moistureScore(),
                metrics.oilinessScore(),
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

            String responseBody = fastApiClient.post()
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
            log.warn("[SkinAnalysis] FastAPI 정량 분석 호출 실패: {}", e.getMessage());
            return QuantitativeMetrics.empty();
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

    private Integer readNullableInt(JsonNode root, String key) {
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