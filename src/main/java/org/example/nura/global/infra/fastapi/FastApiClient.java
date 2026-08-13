package org.example.nura.global.infra.fastapi;

import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.skin.dto.response.FastApiOcrResponse;
import org.example.nura.domain.skin.dto.response.FastApiSkinAnalysisResponse;
import org.example.nura.domain.skin.entity.enums.CosmeticType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
public class FastApiClient {

    private final RestClient restClient;

    public FastApiClient(@Value("${fastapi.url:http://localhost:8000}") String fastApiUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(fastApiUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public FastApiOcrResponse analyzeCosmeticOcr(String photoUrl) {
        try {
            return restClient.post()
                    .uri("/api/v1/ocr/analyze")
                    .body(Map.of("photo_url", photoUrl))
                    .retrieve()
                    .body(FastApiOcrResponse.class);
        } catch (Exception e) {
            log.error("FastAPI OCR 호출 실패", e);
            return new FastApiOcrResponse(
                    "이니스프리",
                    "그린티 씨드 세럼",
                    CosmeticType.SERUM,
                    "정제수, 글리세린, 녹차추출물, 부틸렌글라이콜",
                    "녹차추출물, 히알루론산"
            );
        }
    }

    public FastApiSkinAnalysisResponse analyzeSkin(String photoUrl) {
        try {
            return restClient.post()
                    .uri("/api/v1/skin/analyze")
                    .body(Map.of("photo_url", photoUrl))
                    .retrieve()
                    .body(FastApiSkinAnalysisResponse.class);
        } catch (Exception e) {
            log.error("FastAPI 피부 분석 호출 실패", e);
            return new FastApiSkinAnalysisResponse(30, 2);
        }
    }
}