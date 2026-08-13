package org.example.nura.global.infra.clova;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public String extractTextFromUrl(String imageUrl) {
        if (secretKey == null || secretKey.isBlank()) {
            log.error("[ClovaOCR] Secret Key가 설정되지 않았습니다.");
            throw new BaseException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "OCR 서비스 설정 오류가 발생했습니다."
            );
        }

        try {
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
                throw new BaseException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "Clova OCR 응답이 비어있습니다."
                );
            }

            StringBuilder builder = new StringBuilder();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode fields = root.path("images").path(0).path("fields");

            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    builder.append(field.path("inferText").asText()).append(" ");
                }
            }

            String extractedText = builder.toString().trim();
            if (extractedText.isBlank()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "이미지에서 텍스트를 읽을 수 없습니다."
                );
            }

            return extractedText;

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ClovaOCR] OCR 텍스트 추출 중 오류 발생", e);
            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "Clova OCR 텍스트 추출에 실패했습니다."
            );
        }
    }
}