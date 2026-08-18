package org.example.nura.domain.skin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkinAnalysisService {

    private final OpenAiService openAiService;
    private final DutyScheduleRepository dutyScheduleRepository;
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
            Long userId,
            LocalDate checkinDate,
            MultipartFile photo,
            Integer fatigue,
            Integer tightness,
            Integer redness
    ) {
        // 최근 5일간의 근무 스케줄 조회 및 문맥 가공
        LocalDate endDate = checkinDate != null ? checkinDate : LocalDate.now();
        LocalDate startDate = endDate.minusDays(4);

        List<DutySchedule> recentSchedules = dutyScheduleRepository
                .findAllByUserIdAndDateBetweenOrderByDateAsc(userId, startDate, endDate);

        String scheduleContext = buildScheduleContext(recentSchedules, startDate, endDate);

        if (photo == null || photo.isEmpty()) {
            OpenAiService.AiAnalysisOutput aiOutput = openAiService.generateAiAnalysis(
                    fatigue, tightness, redness,
                    null, null, null, null,
                    SkinAnalysisLevel.UNKNOWN, SkinAnalysisLevel.UNKNOWN, SkinAnalysisLevel.UNKNOWN, SkinAnalysisLevel.UNKNOWN,
                    scheduleContext
            );

            return new AnalysisResult(
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    SkinAnalysisLevel.UNKNOWN,
                    aiOutput.getAiComment(),
                    aiOutput.getRednessComment(),
                    aiOutput.getMoistureComment(),
                    aiOutput.getTroubleComment(),
                    aiOutput.getTags()
            );
        }

        QuantitativeMetrics metrics = requestQuantitativeMetrics(photo);

        SkinAnalysisLevel analyzedRedness = mapScoreToLevel(metrics.rednessScore());
        SkinAnalysisLevel analyzedMoisture = mapScoreToLevel(metrics.moistureScore());
        SkinAnalysisLevel analyzedOiliness = mapScoreToLevel(metrics.oilinessScore());
        SkinAnalysisLevel analyzedTrouble = mapAcneToLevel(metrics.acneCount());

        OpenAiService.AiAnalysisOutput aiOutput = openAiService.generateAiAnalysis(
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
                analyzedTrouble,
                scheduleContext
        );

        return new AnalysisResult(
                analyzedRedness,
                analyzedMoisture,
                analyzedOiliness,
                analyzedTrouble,
                aiOutput.getAiComment(),
                aiOutput.getRednessComment(),
                aiOutput.getMoistureComment(),
                aiOutput.getTroubleComment(),
                aiOutput.getTags()
        );
    }

    private String buildScheduleContext(List<DutySchedule> schedules, LocalDate startDate, LocalDate endDate) {
        if (schedules.isEmpty()) {
            return "최근 등록된 근무 스케줄 정보가 없습니다.";
        }

        return schedules.stream()
                .map(s -> s.getDate() + ": " + s.getShiftType().name() + " 근무")
                .collect(Collectors.joining("\n"));
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
        if (score == null) return SkinAnalysisLevel.UNKNOWN;
        if (score <= 33) return SkinAnalysisLevel.LOW;
        if (score <= 66) return SkinAnalysisLevel.MEDIUM;
        return SkinAnalysisLevel.HIGH;
    }

    private SkinAnalysisLevel mapAcneToLevel(Integer acneCount) {
        if (acneCount == null) return SkinAnalysisLevel.UNKNOWN;
        if (acneCount == 0) return SkinAnalysisLevel.LOW;
        if (acneCount <= 3) return SkinAnalysisLevel.MEDIUM;
        return SkinAnalysisLevel.HIGH;
    }

    public record AnalysisResult(
            SkinAnalysisLevel analyzedRedness,
            SkinAnalysisLevel analyzedMoisture,
            SkinAnalysisLevel analyzedOiliness,
            SkinAnalysisLevel analyzedTrouble,
            String aiComment,
            String rednessComment,
            String moistureComment,
            String troubleComment,
            String tags
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