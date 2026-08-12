package org.example.nura.domain.schedule.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.response.DutyScheduleDayResponse;
import org.example.nura.domain.schedule.dto.response.DutyScheduleWeeklyResponse;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DutyScheduleService {

    private final DutyScheduleRepository dutyScheduleRepository;

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
                            DutySchedule schedule = scheduleMap.get(date);

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

    private void validatePeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate == null || endDate == null) {
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
}
