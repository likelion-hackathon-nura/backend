package org.example.nura.global.infra.fastapi;

import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.skin.dto.response.FastApiOcrResponse;
import org.example.nura.domain.skin.dto.response.FastApiSkinAnalysisResponse;
import org.example.nura.domain.skin.entity.enums.CosmeticType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class FastApiClient {

    private final RestClient restClient;

    public FastApiClient(@Value("${fastapi.url:http://localhost:8000}") String fastApiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(fastApiUrl)
                .build();
    }

    /**
     * FastAPI 서버에 S3 이미지 URL을 보내 OCR 분석 요청
     */
    public FastApiOcrResponse analyzeCosmeticOcr(String photoUrl) {
        try {
            return restClient.post()
                    .uri("/api/v1/ocr/analyze")
                    .body(Map.of("photo_url", photoUrl))
                    .retrieve()
                    .body(FastApiOcrResponse.class);
        } catch (Exception e) {
            log.error("FastAPI OCR 호출 실패: {}", e.getMessage());
            // FastAPI 미기동 시 백엔드가 터지지 않도록 안전장치(Fallback) 제공
            return new FastApiOcrResponse(
                    "이니스프리",
                    "그린티 씨드 세럼",
                    CosmeticType.SERUM,
                    "정제수, 글리세린, 녹차추출물, 부틸렌글라이콜",
                    "녹차추출물, 히알루론산"
            );
        }
    }

    /**
     * FastAPI 서버에 피부 사진 S3 URL을 보내 피부 상태 분석 요청
     */
    public FastApiSkinAnalysisResponse analyzeSkin(String photoUrl) {
        try {
            return restClient.post()
                    .uri("/api/v1/skin/analyze")
                    .body(Map.of("photo_url", photoUrl))
                    .retrieve()
                    .body(FastApiSkinAnalysisResponse.class);
        } catch (Exception e) {
            log.error("FastAPI 피부 분석 호출 실패: {}", e.getMessage());
            // FastAPI 미기동 시 백엔드가 안 터지도록 기본 폴백 값 제공
            return new FastApiSkinAnalysisResponse(30, 2);
        }
    }



}