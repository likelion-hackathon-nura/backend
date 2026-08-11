package org.example.nura.global.infra.clova;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ClovaOcrService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${clova.ocr.secret-key:}")
    private String secretKey;

    public ClovaOcrService(@Value("${clova.ocr.api-url:}") String apiUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(apiUrl.isBlank() ? "https://example.com" : apiUrl)
                .build();
    }

    public String extractTextFromUrl(String imageUrl) {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("[ClovaOCR] Secret Key가 설정되지 않았습니다.");
            return "";
        }

        try {
            // Clova OCR General API 요청 Body 구성
            Map<String, Object> requestBody = Map.of(
                    "version", "V2",
                    "requestId", UUID.randomUUID().toString(),
                    "timestamp", System.currentTimeMillis(),
                    "images", List.of(
                            Map.of(
                                    "format", "jpg",
                                    "name", "cosmetic_label",
                                    "url", imageUrl
                            )
                    )
            );

            String responseBody = restClient.post()
                    .header("X-OCR-SECRET", secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                return "";
            }

            // OCR 결과 텍스트 하나로 병합
            StringBuilder builder = new StringBuilder();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode fields = root.path("images").path(0).path("fields");

            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    builder.append(field.path("inferText").asText()).append(" ");
                }
            }

            return builder.toString().trim();

        } catch (Exception e) {
            log.error("[ClovaOCR] OCR 텍스트 추출 실패: {}", e.getMessage());
            return "";
        }
    }
}