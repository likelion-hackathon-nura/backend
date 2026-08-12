package org.example.nura.global.infra.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${openai.base-url}")
    private String baseUrl;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    public String chat(
            String systemPrompt,
            String userPrompt
    ) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                ),
                "temperature", 0
        );

        try {
            return restClientBuilder
                    .build()
                    .post()
                    .uri(baseUrl + "/chat/completions")
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

        } catch (RestClientResponseException e) {

            log.error(
                    "OpenAI API 오류 - status={}, body={}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );

            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "AI 요청 처리에 실패했습니다."
            );

        } catch (Exception e) {

            log.error(
                    "OpenAI API 호출 중 오류",
                    e
            );

            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "AI 요청 처리에 실패했습니다."
            );
        }
    }
}
