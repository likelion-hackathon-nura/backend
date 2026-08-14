package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DutyScheduleOcrParser {

    private final ObjectMapper objectMapper;

    public String extractStructuredText(
            String rawJson
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(rawJson);

            JsonNode images =
                    root.path("images");

            if (!images.isArray()
                    || images.isEmpty()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "OCR 이미지 분석 결과가 없습니다."
                );
            }

            JsonNode fields =
                    images.get(0)
                            .path("fields");

            if (!fields.isArray()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "OCR 텍스트 분석 결과가 없습니다."
                );
            }

            List<OcrField> ocrFields =
                    new ArrayList<>();

            for (JsonNode field : fields) {

                String text =
                        field.path("inferText")
                                .asText();

                if (text == null
                        || text.isBlank()) {
                    continue;
                }

                JsonNode vertices =
                        field.path("boundingPoly")
                                .path("vertices");

                if (!vertices.isArray()
                        || vertices.isEmpty()) {
                    continue;
                }

                double x =
                        vertices.get(0)
                                .path("x")
                                .asDouble();

                double y =
                        vertices.get(0)
                                .path("y")
                                .asDouble();

                ocrFields.add(
                        new OcrField(
                                text,
                                x,
                                y
                        )
                );
            }

            ocrFields.sort(
                    Comparator
                            .comparingDouble(OcrField::y)
                            .thenComparingDouble(OcrField::x)
            );

            StringBuilder result =
                    new StringBuilder();

            for (OcrField field : ocrFields) {

                result.append("text=")
                        .append(field.text())
                        .append(", x=")
                        .append((int) field.x())
                        .append(", y=")
                        .append((int) field.y())
                        .append("\n");
            }

            return result.toString();

        } catch (BaseException e) {
            throw e;

        } catch (Exception e) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "OCR 결과를 해석할 수 없습니다."
            );
        }
    }

    private record OcrField(
            String text,
            double x,
            double y
    ) {
    }
}
