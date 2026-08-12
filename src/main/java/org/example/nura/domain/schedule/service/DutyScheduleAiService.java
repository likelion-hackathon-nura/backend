package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.prompt.DutySchedulePrompt;
import org.example.nura.global.infra.openai.OpenAiClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DutyScheduleAiService {

    private final OpenAiClient openAiClient;

    public String parseDutySchedule(
            String ocrText
    ) {
        return openAiClient.chat(
                DutySchedulePrompt.SYSTEM_PROMPT,
                ocrText
        );
    }
}
