package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nura.domain.home.service.HomeService;
import org.example.nura.global.infra.ocr.DutyScheduleOcrClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.example.nura.domain.schedule.dto.request.DutyScheduleItemRequest;
import org.example.nura.domain.schedule.dto.request.DutyScheduleSaveRequest;
import org.example.nura.domain.schedule.dto.response.DutyScheduleDayResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleWeeklyResponse;
import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DailyTimeAllocationRepository;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.schedule.repository.TimeBlockRepository;
import org.example.nura.domain.schedule.service.overlay.TimeBlockOverlayService;
import org.example.nura.domain.schedule.service.plan.DailyPlanSummaryCalculator;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.example.nura.domain.schedule.dto.response.DutyScheduleOcrItemResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleOcrResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DutyScheduleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_PERIOD_DAYS = 90;

    private final DutyScheduleRepository dutyScheduleRepository;
    private final UserRepository userRepository;
    private final DailyTimeAllocationRepository dailyTimeAllocationRepository;
    private final TimeBlockRepository timeBlockRepository;

    private final DutyScheduleAiService dutyScheduleAiService;
    private final DutyScheduleOcrClient dutyScheduleOcrClient;
    private final DutyScheduleOcrParser dutyScheduleOcrParser;
    private final DailyPlanSummaryCalculator dailyPlanSummaryCalculator;
    private final TimeBlockOverlayService timeBlockOverlayService;
    private final ObjectMapper objectMapper;
    private final HomeService homeService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DutyScheduleOcrResponse recognizeSchedule(
            Long userId,
            MultipartFile image
    ) {
        String rawJson =
                dutyScheduleOcrClient.analyze(image);

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

        if (request.schedules() == null || request.schedules().isEmpty()) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "저장할 근무표가 없습니다."
            );
        }

        LocalDate today = LocalDate.now(KST);

        // date null / 과거 날짜 검증
        for (DutyScheduleItemRequest item : request.schedules()) {
            validateDate(item.date(), today);
        }

        validateDuplicateDates(request);

        List<LocalDate> dates = request.schedules().stream()
                .map(DutyScheduleItemRequest::date)
                .toList();

        LocalDate minDate = dates.stream()
                .min(LocalDate::compareTo)
                .orElseThrow();

        LocalDate maxDate = dates.stream()
                .max(LocalDate::compareTo)
                .orElseThrow();

        Map<LocalDate, DutySchedule> existingMap =
                dutyScheduleRepository
                        .findAllByUserIdAndDateBetweenOrderByDateAsc(
                                userId, minDate, maxDate
                        )
                        .stream()
                        .collect(Collectors.toMap(
                                DutySchedule::getDate,
                                Function.identity()
                        ));

        List<DutySchedule> toSave = new ArrayList<>();

        for (DutyScheduleItemRequest item : request.schedules()) {

            LocalDate date = item.date();
            ShiftType shiftType = item.shiftType();

            DutySchedule existing = existingMap.get(date);

            // 오늘 근무는 기존 데이터가 있으면 수정 불가
            if (date.equals(today) && existing != null) {
                throw new BaseException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "오늘 등록된 근무는 수정할 수 없습니다."
                );
            }

            ShiftTime shiftTime = resolveShiftTime(user, shiftType);
            validateShiftTimeConfigured(shiftType, shiftTime);

            if (existing == null) {
                toSave.add(DutySchedule.create(
                        user,
                        date,
                        shiftType,
                        shiftTime.startTime(),
                        shiftTime.endTime(),
                        request.source()
                ));
            } else {
                // 미래 날짜 기존 데이터 수정 - OCR로 같은 미래 날짜를 다시 확정해도 새로운 값으로 덮어씀
                existing.update(
                        shiftType,
                        shiftTime.startTime(),
                        shiftTime.endTime(),
                        request.source()
                );
            }
        }

        try {
            dutyScheduleRepository.saveAll(toSave);
            dutyScheduleRepository.flush();

            if (containsToday(
                    request,
                    today
            )) {
                syncTodayHome(
                        userId,
                        today
                );
            }

        } catch (DataIntegrityViolationException e) {

            log.warn(
                    "근무표 저장 중 중복 데이터 발생 - userId={}",
                    userId,
                    e
            );

            throw new BaseException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "이미 등록된 날짜의 근무가 포함되어 있습니다."
            );
        }
    }

    private void syncTodayHome(
            Long userId,
            LocalDate today
    ) {
        DailyTimeAllocation allocation =
                dailyTimeAllocationRepository
                        .findByUserIdAndDate(
                                userId,
                                today
                        )
                        .orElse(null);

        if (allocation == null) {
            homeService.ensureToday(userId);
            return;
        }

        List<DutySchedule> schedules =
                dutyScheduleRepository
                        .findAllByUserIdAndDateBetweenOrderByDateAsc(
                                userId,
                                today.minusDays(1),
                                today
                        );

        List<TimeBlock> currentBlocks =
                timeBlockRepository
                        .findAllByAllocationIdOrderByStartAtAsc(
                                allocation.getId()
                        );
        LocalDateTime dayStart =
                today.atStartOfDay();

        LocalDateTime dayEnd =
                today.plusDays(1).atStartOfDay();

        for (DutySchedule schedule : schedules) {
            if (schedule.getShiftType() == ShiftType.OFF
                    || schedule.getStartTime() == null
                    || schedule.getEndTime() == null) {
                continue;
            }

            TimeInterval workInterval =
                    clipScheduleToToday(
                            schedule,
                            dayStart,
                            dayEnd
                    );

            if (workInterval == null) {
                continue;
            }

            TimeBlockOverlayService.OverlayResult overlayResult =
                    timeBlockOverlayService.overlay(
                            allocation,
                            currentBlocks,
                            TimeCategory.SOCIAL,
                            "근무",
                            workInterval.startAt(),
                            workInterval.endAt(),
                            null,
                            Map.of(),
                            TimeBlockSource.SCHEDULE
                    );

            if (!overlayResult.blocksToDelete().isEmpty()) {
                timeBlockRepository.deleteAll(
                        overlayResult.blocksToDelete()
                );
            }

            if (!overlayResult.blocksToInsert().isEmpty()) {
                timeBlockRepository.saveAll(
                        overlayResult.blocksToInsert()
                );
            }

            currentBlocks =
                    timeBlockRepository
                            .findAllByAllocationIdOrderByStartAtAsc(
                                    allocation.getId()
                            );
        }

        List<PlannedTimeBlock> plannedBlocks =
                currentBlocks.stream()
                        .map(block ->
                                new PlannedTimeBlock(
                                        block.getCategory(),
                                        block.getLabel(),
                                        block.getStartAt(),
                                        block.getEndAt(),
                                        block.getSource(),
                                        block.getCustomEvent()
                                )
                        )
                        .toList();

        DailyPlanSummaryCalculator.DailyPlanSummary summary =
                dailyPlanSummaryCalculator.calculate(
                        plannedBlocks
                );

        allocation.updateAllocation(
                summary.socialMinutes(),
                summary.refreshMinutes(),
                summary.myMinutes(),
                allocation.getAiComment()
        );
    }

    private TimeInterval clipScheduleToToday(
            DutySchedule schedule,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        LocalDateTime startAt =
                schedule.getDate().atTime(
                        schedule.getStartTime()
                );

        LocalDateTime endAt =
                schedule.getDate().atTime(
                        schedule.getEndTime()
                );

        if (!endAt.isAfter(startAt)) {
            endAt = endAt.plusDays(1);
        }

        LocalDateTime clippedStart =
                startAt.isBefore(dayStart)
                        ? dayStart
                        : startAt;

        LocalDateTime clippedEnd =
                endAt.isAfter(dayEnd)
                        ? dayEnd
                        : endAt;

        if (!clippedEnd.isAfter(clippedStart)) {
            return null;
        }

        return new TimeInterval(
                clippedStart,
                clippedEnd
        );
    }

    private boolean containsToday(
            DutyScheduleSaveRequest request,
            LocalDate today
    ) {
        return request.schedules().stream()
                .anyMatch(item ->
                        today.equals(item.date())
                );
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
                            .asString("");

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

            // 1차 파싱: 유효한 날짜만 추출해 범위 조회
            List<LocalDate> parsedDates = new ArrayList<>();
            for (JsonNode node : schedulesNode) {
                try {
                    parsedDates.add(LocalDate.parse(node.path("date").asString("")));
                } catch (Exception ignored) {
                    // 파싱 불가 항목은 건너뜀
                }
            }

            Map<LocalDate, DutySchedule> existingMap = parsedDates.isEmpty()
                    ? Map.of()
                    : dutyScheduleRepository
                            .findAllByUserIdAndDateBetweenOrderByDateAsc(
                                    userId,
                                    parsedDates.stream().min(LocalDate::compareTo).orElseThrow(),
                                    parsedDates.stream().max(LocalDate::compareTo).orElseThrow()
                            )
                            .stream()
                            .collect(Collectors.toMap(
                                    DutySchedule::getDate,
                                    Function.identity()
                            ));

            List<DutyScheduleOcrItemResponse> schedules =
                    new ArrayList<>();

            for (JsonNode scheduleNode : schedulesNode) {

                try {
                    LocalDate date =
                            LocalDate.parse(
                                    scheduleNode
                                            .path("date")
                                            .asString("")
                            );

                    ShiftType ocrShiftType =
                            ShiftType.valueOf(
                                    scheduleNode
                                            .path("shiftType")
                                            .asString("")
                            );

                    DutySchedule existing = existingMap.get(date);

                    // 과거 날짜와 이미 등록된 오늘 날짜는 제외
                    if (date.isBefore(today)
                            || (date.equals(today) && existing != null)) {
                        continue;
                    }

                    schedules.add(
                            new DutyScheduleOcrItemResponse(
                                    date,
                                    date.getDayOfWeek(),
                                    ocrShiftType
                            )
                    );

                } catch (Exception e) {
                    log.warn("OCR 항목 해석 실패, 건너뜀: {}", scheduleNode, e);
                }
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

        if (startDate.until(endDate, java.time.temporal.ChronoUnit.DAYS) >= MAX_PERIOD_DAYS) {
            throw new BaseException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "조회 기간은 최대 " + MAX_PERIOD_DAYS + "일까지 가능합니다."
            );
        }
    }

    private record ShiftTime(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
