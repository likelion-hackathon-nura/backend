package org.example.nura.domain.schedule.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.request.DutyScheduleSaveRequest;
import org.example.nura.domain.schedule.dto.response.DutyScheduleOcrResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleWeeklyResponse;
import org.example.nura.domain.schedule.service.DutyScheduleService;
import org.example.nura.global.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Tag(name = "근무표 관리", description = "근무표 조회 및 등록/수정 API")
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class DutyScheduleController {

    private final DutyScheduleService dutyScheduleService;

    @Operation(
            summary = "근무표 기간 조회",
            description = "지정한 시작일과 종료일 사이의 근무표를 조회합니다."
    )
    @GetMapping
    public ApiResponse<DutyScheduleWeeklyResponse> getSchedules(
            @AuthenticationPrincipal Long userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        DutyScheduleWeeklyResponse response =
                dutyScheduleService.getSchedules(
                        userId,
                        startDate,
                        endDate
                );

        return ApiResponse.success(
                "근무표 조회에 성공했습니다.",
                response
        );
    }

    @Operation(
            summary = "근무표 일괄 등록/수정",
            description = "근무표를 일괄 저장합니다. 기존 데이터가 없는 날짜는 신규 등록하고, 수정 가능한 기존 데이터는 갱신합니다."
    )
    @PutMapping
    public ApiResponse<Void> saveSchedules(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DutyScheduleSaveRequest request
    ) {
        dutyScheduleService.saveSchedules(userId, request);

        return ApiResponse.success(
                "근무표가 저장되었습니다.",
                null
        );
    }

    @Operation(
            summary = "근무표 OCR 인식",
            description = "근무표 이미지를 분석하여 날짜별 근무 형태를 반환합니다. 현재 시점 기준 등록 가능한 일정만 반환합니다.(과거, 오늘 이미 등록된 일정은 제외)"
    )
    @PostMapping(
            value = "/ocr",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<DutyScheduleOcrResponse> recognizeSchedule(
            @AuthenticationPrincipal Long userId,
            @RequestPart("image") MultipartFile image
    ) {
        DutyScheduleOcrResponse response =
                dutyScheduleService.recognizeSchedule(
                        userId,
                        image
                );

        return ApiResponse.success(
                "근무표 인식이 완료되었습니다.",
                response
        );
    }
}
