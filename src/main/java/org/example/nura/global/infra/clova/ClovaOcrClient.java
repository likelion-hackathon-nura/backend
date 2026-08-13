package org.example.nura.global.infra.clova;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ClovaOcrClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${clova.ocr.api-url}")
    private String apiUrl;

    @Value("${clova.ocr.secret-key}")
    private String secretKey;

    public ClovaOcrClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public String analyze(MultipartFile image) {
        validateImage(image);

        try {
            ByteArrayResource imageResource =
                    new ByteArrayResource(image.getBytes()) {
                        @Override
                        public String getFilename() {
                            return image.getOriginalFilename() == null
                                    ? "image.jpg"
                                    : image.getOriginalFilename();
                        }
                    };

            Map<String, Object> message = Map.of(
                    "version", "V2",
                    "requestId", UUID.randomUUID().toString(),
                    "timestamp", System.currentTimeMillis(),
                    "lang", "ko",
                    "images", List.of(
                            Map.of(
                                    "format", resolveFormat(image),
                                    "name", "cosmetic-image"
                            )
                    )
            );

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("message", objectMapper.writeValueAsString(message));
            body.add("file", imageResource);

            return restClient.post()
                    .uri(apiUrl)
                    .header("X-OCR-SECRET", secretKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException e) {
            log.error(
                    "CLOVA OCR API 오류 - status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString(),
                    e
            );

            if (e.getStatusCode().is4xxClientError()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "화장품 OCR 처리에 실패했습니다."
                );
            }

            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OCR 서비스에 일시적인 오류가 발생했습니다."
            );

        } catch (Exception e) {
            log.error("CLOVA OCR 처리 중 예상하지 못한 오류", e);
            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "OCR 서비스에 연결할 수 없습니다."
            );
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "화장품 이미지는 필수입니다."
            );
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미지 파일만 업로드할 수 있습니다."
            );
        }
    }

    private String resolveFormat(MultipartFile image) {
        String contentType = image.getContentType();
        if ("image/png".equals(contentType)) {
            return "png";
        }
        return "jpg";
    }
}