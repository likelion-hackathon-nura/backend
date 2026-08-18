package org.example.nura.domain.skin.service;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.skin.dto.response.SkinMainTodayResponse;
import org.example.nura.domain.skin.dto.response.SkinMainTodayResponse.WeeklyRecordDto;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.RoutineStep;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.repository.CheckinRepository;
import org.example.nura.domain.skin.repository.RoutineStepRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkinMainService {

    private final CheckinRepository checkinRepository;
    private final SkinRoutineRepository skinRoutineRepository;
    private final RoutineStepRepository routineStepRepository;

    public SkinMainTodayResponse getTodayMain(Long userId, LocalDate today) {
        // 1. 오늘 체크인 여부 조회
        Optional<Checkin> todayCheckinOpt = checkinRepository.findByUserIdAndDate(userId, today);
        boolean isCheckedIn = todayCheckinOpt.isPresent();

        // 2. 연속 체크인 일수(Streak) 계산
        int streakDays = calculateStreak(userId, today, isCheckedIn);

        // 3. 이번 주(월~일) 체크인 기록 생성
        List<WeeklyRecordDto> weeklyRecords = getWeeklyRecords(userId, today);

        // 4. 체크인을 아직 안 한 경우
        if (!isCheckedIn) {
            return SkinMainTodayResponse.builder()
                    .isCheckedIn(false)
                    .isRoutineCompleted(false)
                    .streakDays(streakDays)
                    .weeklyRecords(weeklyRecords)
                    .checkinSummary(null)
                    .routineSummary(null)
                    .build();
        }

        // 5. 체크인 완료 시: 오늘 체크인 및 루틴 정보 조회
        Checkin checkin = todayCheckinOpt.get();
        SkinMainTodayResponse.CheckinSummaryDto checkinSummary =
                SkinMainTodayResponse.CheckinSummaryDto.from(checkin);

        Optional<SkinRoutine> routineOpt =
                skinRoutineRepository.findByCheckinUserIdAndCheckinDate(userId, today);

        // 루틴 스텝 및 제품 정보 조회 (오른쪽 카드 렌더링용)
        List<RoutineStep> steps = routineOpt
                .map(r -> routineStepRepository.findAllByRoutineIdOrderByStepOrderAsc(r.getId()))
                .orElse(List.of());

        SkinMainTodayResponse.RoutineSummaryDto routineSummary =
                routineOpt.map(r -> SkinMainTodayResponse.RoutineSummaryDto.of(r, steps)).orElse(null);

        // 오늘 3분 회복 모드 완료 여부 판단
        boolean isRoutineCompleted = routineOpt.map(SkinRoutine::isCompleted).orElse(false);

        return SkinMainTodayResponse.builder()
                .isCheckedIn(true)
                .isRoutineCompleted(isRoutineCompleted)
                .streakDays(streakDays)
                .weeklyRecords(weeklyRecords)
                .checkinSummary(checkinSummary)
                .routineSummary(routineSummary)
                .build();
    }

    private int calculateStreak(Long userId, LocalDate today, boolean isCheckedInToday) {
        int streak = 0;
        LocalDate targetDate = isCheckedInToday ? today : today.minusDays(1);

        while (checkinRepository.existsByUserIdAndDate(userId, targetDate)) {
            streak++;
            targetDate = targetDate.minusDays(1);
        }

        return streak;
    }

    private List<WeeklyRecordDto> getWeeklyRecords(Long userId, LocalDate today) {
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate sunday = today.with(DayOfWeek.SUNDAY);

        // 1. 주간 체크인 목록 조회
        List<Checkin> weeklyCheckins =
                checkinRepository.findAllByUserIdAndDateBetweenOrderByDateAsc(userId, monday, sunday);

        Set<LocalDate> checkedDates = weeklyCheckins.stream()
                .map(Checkin::getDate)
                .collect(Collectors.toSet());

        // 2. 주간 루틴 목록 조회 (새로 추가한 Repository 메서드 사용)
        List<SkinRoutine> weeklyRoutines =
                skinRoutineRepository.findAllByUserIdAndDateBetween(userId, monday, sunday);

        // 3. 완료된(completed = true) 루틴의 날짜 Set 추출
        Set<LocalDate> completedRoutineDates = weeklyRoutines.stream()
                .filter(SkinRoutine::isCompleted)
                .map(r -> r.getCheckin().getDate())
                .collect(Collectors.toSet());

        // 4. 주간 날짜별 상태 결정 (COMPLETED / CHECKED_IN / NONE)
        List<WeeklyRecordDto> records = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            boolean isChecked = checkedDates.contains(date);
            boolean isRoutineDone = completedRoutineDates.contains(date);

            String status = "NONE";
            if (isRoutineDone) {
                status = "COMPLETED";     // 체크인 & 회복 루틴 완료
            } else if (isChecked) {
                status = "CHECKED_IN";    // 체크인만 완료
            }

            records.add(WeeklyRecordDto.builder()
                    .dayOfWeek(date.getDayOfWeek().name().substring(0, 3))
                    .date(date)
                    .status(status)
                    .build());
        }

        return records;
    } }