// openai 호출해서 추가 refresh time 구성 추천받음
package org.example.nura.domain.schedule.service.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.dto.ai.SkinRecoveryPlan;
import org.example.nura.domain.schedule.prompt.DailyRefreshPrompt;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.openai.OpenAiClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyRefreshAiService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    private final RefreshPlanValidator refreshPlanValidator;

    public RefreshPlanAiResponse recommend(
            RefreshPlanAiRequest request
    ) {
        try {
            String userPrompt =
                    objectMapper.writeValueAsString(request);

            String rawResponse =
                    openAiClient.chat(
                            DailyRefreshPrompt.SYSTEM_PROMPT,
                            userPrompt
                    );

            RefreshPlanAiResponse response =
                    parseResponse(rawResponse);

            refreshPlanValidator.validate(
                    request,
                    response
            );

            return response;

        } catch (Exception e) {

            log.warn(
                    "Refresh AI 추천 실패 - fallback 계획 사용",
                    e
            );

            return createFallbackResponse();
        }
    }

    private RefreshPlanAiResponse parseResponse(
            String rawResponse
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(rawResponse);

            String content =
                    root.path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asString("");

            if (content.isBlank()) {
                throw new BaseException(
                        ErrorCode.EXTERNAL_API_ERROR,
                        "회복 시간 추천 결과가 비어 있습니다."
                );
            }

            return objectMapper.readValue(
                    content,
                    RefreshPlanAiResponse.class
            );

        } catch (BaseException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "Refresh AI 응답 파싱 실패 - response={}",
                    rawResponse,
                    e
            );

            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "회복 시간 추천 결과를 해석할 수 없습니다."
            );
        }
    }

    private RefreshPlanAiResponse createFallbackResponse() {
        return new RefreshPlanAiResponse(
                List.of(),
                new SkinRecoveryPlan(
                        false,
                        null,
                        null
                )
        );
    }
}
