// openai 호출해서 추가 refresh time 구성 추천받음
package org.example.nura.domain.schedule.service.refresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiRequest;
import org.example.nura.domain.schedule.dto.ai.RefreshPlanAiResponse;
import org.example.nura.domain.schedule.prompt.DailyRefreshPrompt;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.openai.OpenAiClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

            RefreshPlanAiResponse response = parseResponse(rawResponse);
            refreshPlanValidator.validate(request, response);
            return response;

        } catch (BaseException e) {
            throw e;

        } catch (Exception e) {
            log.error(
                    "Refresh AI 추천 처리 중 오류",
                    e
            );

            throw new BaseException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "회복 시간 추천에 실패했습니다."
            );
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
}
