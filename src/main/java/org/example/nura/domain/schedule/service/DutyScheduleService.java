package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.request.DutyScheduleItemRequest;
import org.example.nura.domain.schedule.dto.request.DutyScheduleSaveRequest;
import org.example.nura.domain.schedule.dto.response.DutyScheduleDayResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleWeeklyResponse;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.example.nura.global.infra.ocr.ClovaOcrClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.example.nura.domain.schedule.dto.response.DutyScheduleOcrItemResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleOcrResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DutyScheduleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DutyScheduleRepository dutyScheduleRepository;
    private final UserRepository userRepository;

    private final DutyScheduleAiService dutyScheduleAiService;
    private final ClovaOcrClient clovaOcrClient;
    private final DutyScheduleOcrParser dutyScheduleOcrParser;
    private final ObjectMapper objectMapper;

    public DutyScheduleOcrResponse recognizeSchedule(
            Long userId,
            MultipartFile image
    ) {
        String rawJson =
                clovaOcrClient.analyze(image);

        String structuredText =
                dutyScheduleOcrParser.extractStructuredText(rawJson);

        String openAiRawResponse =
                dutyScheduleAiService.parseDutySchedule(
                        structuredText
                );

        return parseOpenAiResponse(
                userId,
                openAiRawResponse
        );
    }

    // 근무표 기간 조회
    public DutyScheduleWeeklyResponse getSchedules(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        validatePeriod(startDate, endDate);

        List<DutySchedule> schedules =
                dutyScheduleRepository
                        .findAllByUserIdAndDateBetweenOrderByDateAsc(
                                userId,
                                startDate,
                                endDate
                        );

        Map<LocalDate, DutySchedule> scheduleMap =
                schedules.stream()
                        .collect(Collectors.toMap(
                                DutySchedule::getDate,
                                Function.identity()
                        ));

        List<DutyScheduleDayResponse> responses =
                startDate.datesUntil(endDate.plusDays(1))
                        .map(date -> {
                            DutySchedule schedule =
                                    scheduleMap.get(date);

                            ShiftType shiftType =
                                    schedule == null
                                            ? null
                                            : schedule.getShiftType();

                            return new DutyScheduleDayResponse(
                                    date,
                                    date.getDayOfWeek(),
                                    shiftType
                            );
                        })
                        .toList();

        return new DutyScheduleWeeklyResponse(responses);
    }

    // 근무표 일괄 등록/수정
    // - OCR 확정 결과 저장
    // - 직접 작성 신규 등록
    // - 직접 작성 미래 근무 수정
    @Transactional
    public void saveSchedules(
            Long userId,
            DutyScheduleSaveRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.RESOURCE_NOT_FOUND
                        )
                );

        if (!user.isOnboardingCompleted()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "온보딩 완료 후 근무표를 등록할 수 있습니다."
            );
        }

        validateDuplicateDates(request);

        LocalDate today = LocalDate.now(KST);

        for (DutyScheduleItemRequest item : request.schedules()) {

            LocalDate date = item.date();
            ShiftType shiftType = item.shiftType();

            validateDate(date, today);

            DutySchedule existing =
                    dutyScheduleRepository
                            .findByUserIdAndDate(
                                    userId,
                                    date
                            )
                            .orElse(null);

            // 오늘 근무는 기존 데이터가 있으면 수정 불가
            if (date.equals(today)
                    && existing != null) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "오늘 등록된 근무는 수정할 수 없습니다."
                );
            }

            ShiftTime shiftTime =
                    resolveShiftTime(
                            user,
                            shiftType
                    );

            validateShiftTimeConfigured(
                    shiftType,
                    shiftTime
            );

            // 기존 데이터가 없으면 신규 생성
            if (existing == null) {

                DutySchedule dutySchedule =
                        DutySchedule.create(
                                user,
                                date,
                                shiftType,
                                shiftTime.startTime(),
                                shiftTime.endTime(),
                                request.source()
                        );

                dutyScheduleRepository.save(
                        dutySchedule
                );

                continue;
            }

            // 미래 날짜 기존 데이터 수정 - OCR로 같은 미래 날짜를 다시 확정해도 새로운 값으로 덮어씀
            existing.update(
                    shiftType,
                    shiftTime.startTime(),
                    shiftTime.endTime(),
                    request.source()
            );
        }
    }

    private DutyScheduleOcrResponse parseOpenAiResponse(
            Long userId,
            String rawResponse
    ) {
        try {
            JsonNode root =
                    objectMapper.readTree(rawResponse);

            JsonNode choices =
                    root.path("choices");

            if (!choices.isArray()
                    || choices.isEmpty()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "근무표 분석 결과가 없습니다."
                );
            }

            String content =
                    choices.get(0)
                            .path("message")
                            .path("content")
                            .asText();

            if (content == null
                    || content.isBlank()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "근무표 분석 결과가 비어 있습니다."
                );
            }

            JsonNode contentJson =
                    objectMapper.readTree(content);

            JsonNode schedulesNode =
                    contentJson.path("schedules");

            if (!schedulesNode.isArray()) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "근무표 분석 결과 형식이 올바르지 않습니다."
                );
            }

            LocalDate today =
                    LocalDate.now(KST);

            List<DutyScheduleOcrItemResponse> schedules =
                    new ArrayList<>();

            for (JsonNode scheduleNode : schedulesNode) {

                LocalDate date =
                        LocalDate.parse(
                                scheduleNode
                                        .path("date")
                                        .asText()
                        );

                ShiftType ocrShiftType =
                        ShiftType.valueOf(
                                scheduleNode
                                        .path("shiftType")
                                        .asText()
                        );

                DutySchedule existing =
                        dutyScheduleRepository
                                .findByUserIdAndDate(
                                        userId,
                                        date
                                )
                                .orElse(null);

                ShiftType displayShiftType;
                boolean editable;

                // 과거
                if (date.isBefore(today)) {

                    editable = false;

                    displayShiftType =
                            existing != null
                                    ? existing.getShiftType()
                                    : ocrShiftType;

                    // 오늘 + 기존 근무 있음
                } else if (date.equals(today)
                        && existing != null) {

                    editable = false;

                    displayShiftType =
                            existing.getShiftType();

                    // 오늘 미등록 또는 미래
                } else {

                    editable = true;

                    displayShiftType =
                            ocrShiftType;
                }

                schedules.add(
                        new DutyScheduleOcrItemResponse(
                                date,
                                date.getDayOfWeek(),
                                displayShiftType,
                                editable
                        )
                );
            }

            return new DutyScheduleOcrResponse(
                    schedules
            );

        } catch (BaseException e) {
            throw e;

        } catch (Exception e) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "근무표 분석 결과를 해석할 수 없습니다."
            );
        }
    }


    // D/E/N/OFF에 따라 온보딩에서 저장한 실제 근무 시간을 매핑
    private ShiftTime resolveShiftTime(
            User user,
            ShiftType shiftType
    ) {
        return switch (shiftType) {

            case D -> new ShiftTime(
                    user.getShiftDStart(),
                    user.getShiftDEnd()
            );

            case E -> new ShiftTime(
                    user.getShiftEStart(),
                    user.getShiftEEnd()
            );

            case N -> new ShiftTime(
                    user.getShiftNStart(),
                    user.getShiftNEnd()
            );

            case OFF -> new ShiftTime(
                    null,
                    null
            );
        };
    }


    private void validateShiftTimeConfigured(
            ShiftType shiftType,
            ShiftTime shiftTime
    ) {
        if (shiftType == ShiftType.OFF) {
            return;
        }

        if (shiftTime.startTime() == null
                || shiftTime.endTime() == null) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "근무 시간이 설정되어 있지 않습니다."
            );
        }
    }


    private void validateDate(
            LocalDate date,
            LocalDate today
    ) {
        if (date == null) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "근무 날짜는 필수입니다."
            );
        }

        if (date.isBefore(today)) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "과거 날짜의 근무는 등록하거나 수정할 수 없습니다."
            );
        }
    }

    private void validateDuplicateDates(
            DutyScheduleSaveRequest request
    ) {
        Set<LocalDate> dates =
                new HashSet<>();

        for (DutyScheduleItemRequest item
                : request.schedules()) {

            if (!dates.add(item.date())) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "동일한 날짜가 중복되어 있습니다."
                );
            }
        }
    }

    private void validatePeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null
                || endDate == null) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "조회 시작일과 종료일은 필수입니다."
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "조회 기간이 올바르지 않습니다."
            );
        }
    }

    private record ShiftTime(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
