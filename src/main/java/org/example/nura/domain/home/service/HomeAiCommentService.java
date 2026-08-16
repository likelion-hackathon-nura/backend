package org.example.nura.domain.home.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.home.prompt.HomeAiCommentPrompt;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator.DailyPlanSummary;
import org.example.nura.global.infra.openai.OpenAiClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeAiCommentService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public String generate(
            DailyPlanContext context,
            DailyPlanSummary summary,
            List<PlannedTimeBlock> plannedBlocks
    ) {
        try {
            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "shiftType",
                    context.todayShiftType()
            );

            data.put(
                    "previousShiftType",
                    context.previousShiftType()
            );

            data.put(
                    "consecutiveWorkDays",
                    context.consecutiveWorkDays()
            );

            data.put(
                    "consecutiveNightShiftDays",
                    context.consecutiveNightShiftDays()
            );

            data.put(
                    "targetSleepMinutes",
                    context.targetSleepMinutes()
            );

            data.put(
                    "mealPattern",
                    context.mealPattern()
            );

            data.put(
                    "restActivities",
                    context.restActivities()
            );

            data.put(
                    "balanceScore",
                    context.balanceScore()
            );

            data.put(
                    "sensitivityLevel",
                    context.sensitivityLevel()
            );

            data.put(
                    "skinType",
                    context.skinType()
            );

            data.put(
                    "skinConcerns",
                    context.skinConcerns()
            );

            data.put(
                    "previousCheckin",
                    context.previousCheckin()
            );

            data.put(
                    "previousRecoveryRoutineCompleted",
                    context.previousRecoveryRoutineCompleted()
            );

            data.put(
                    "socialMinutes",
                    summary.socialMinutes()
            );

            data.put(
                    "refreshMinutes",
                    summary.refreshMinutes()
            );

            data.put(
                    "myMinutes",
                    summary.myMinutes()
            );

            // 실제 최종 배치 결과
            data.put(
                    "plannedBlocks",
                    plannedBlocks.stream()
                            .map(block -> {
                                Map<String, Object> blockData =
                                        new LinkedHashMap<>();

                                blockData.put(
                                        "category",
                                        block.category()
                                );

                                blockData.put(
                                        "label",
                                        block.label()
                                );

                                blockData.put(
                                        "startAt",
                                        block.startAt()
                                );

                                blockData.put(
                                        "endAt",
                                        block.endAt()
                                );

                                return blockData;
                            })
                            .toList()
            );

            String userPrompt =
                    objectMapper.writeValueAsString(
                            data
                    );

            String rawResponse =
                    openAiClient.chat(
                            HomeAiCommentPrompt.SYSTEM_PROMPT,
                            userPrompt
                    );

            String content =
                    objectMapper
                            .readTree(rawResponse)
                            .path("choices")
                            .path(0)
                            .path("message")
                            .path("content")
                            .asString("")
                            .trim();

            if (content.isBlank()) {
                return fallbackComment(
                        context
                );
            }

            return content;

        } catch (Exception e) {

            log.warn(
                    "홈 AI 코멘트 생성 실패",
                    e
            );

            return fallbackComment(
                    context
            );
        }
    }

    private String fallbackComment(
            DailyPlanContext context
    ) {
        if (context.consecutiveNightShiftDays() >= 2) {
            return "연속된 나이트 근무를 고려해 오늘은 회복 시간을 충분히 확보했어요.";
        }

        return "오늘의 근무와 생활 패턴을 고려해 회복과 개인 시간을 균형 있게 설계했어요.";
    }
}
