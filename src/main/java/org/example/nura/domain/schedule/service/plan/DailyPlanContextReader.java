// 하루 설계에 필요한 입력 데이터 모음
package org.example.nura.domain.schedule.service.plan;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.context.DailyPlanContext;
import org.example.nura.domain.schedule.dto.context.PreviousCheckinContext;
import org.example.nura.domain.schedule.entity.DutySchedule;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.example.nura.domain.schedule.repository.DutyScheduleRepository;
import org.example.nura.domain.skin.entity.Checkin;
import org.example.nura.domain.skin.entity.SkinRoutine;
import org.example.nura.domain.skin.repository.CheckinRepository;
import org.example.nura.domain.skin.repository.SkinRoutineRepository;
import org.example.nura.domain.user.entity.User;
import org.example.nura.domain.user.entity.UserRestActivity;
import org.example.nura.domain.user.entity.UserSkin;
import org.example.nura.domain.user.entity.UserSkinConcern;
import org.example.nura.domain.user.entity.enums.RestActivityType;
import org.example.nura.domain.user.entity.enums.SkinConcernType;
import org.example.nura.domain.user.repository.UserRepository;
import org.example.nura.domain.user.repository.UserRestActivityRepository;
import org.example.nura.domain.user.repository.UserSkinConcernRepository;
import org.example.nura.domain.user.repository.UserSkinRepository;
import org.example.nura.global.error.ErrorCode;
import org.example.nura.global.error.exception.BaseException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPlanContextReader {

    private final UserRepository userRepository;
    private final DutyScheduleRepository dutyScheduleRepository;
    private final UserRestActivityRepository userRestActivityRepository;

    private final UserSkinRepository userSkinRepository;
    private final UserSkinConcernRepository userSkinConcernRepository;

    private final CheckinRepository checkinRepository;
    private final SkinRoutineRepository skinRoutineRepository;

    public DailyPlanContext read(
            Long userId,
            LocalDate date
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new BaseException(
                                ErrorCode.RESOURCE_NOT_FOUND
                        )
                );

        LocalDate previousDate =
                date.minusDays(1);

        // 전날 체크인 조회
        PreviousCheckinContext previousCheckin =
                checkinRepository
                        .findByUserIdAndDate(
                                userId,
                                previousDate
                        )
                        .map(this::toPreviousCheckinContext)
                        .orElse(null);

        // 전날 회복 루틴 완료 여부 조회
        Boolean previousRecoveryRoutineCompleted =
                skinRoutineRepository
                        .findByCheckinUserIdAndCheckinDate(
                                userId,
                                previousDate
                        )
                        .map(SkinRoutine::isCompleted)
                        .orElse(null);

        // 선호 휴식 활동 조회
        List<RestActivityType> restActivities =
                userRestActivityRepository
                        .findAllByUserId(userId)
                        .stream()
                        .map(UserRestActivity::getActivityType)
                        .toList();

        // 피부 온보딩 정보 조회
        UserSkin userSkin =
                userSkinRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new BaseException(
                                        ErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        // 피부 고민 조회
        List<SkinConcernType> skinConcerns =
                userSkinConcernRepository
                        .findAllByUserSkinId(
                                userSkin.getId()
                        )
                        .stream()
                        .map(UserSkinConcern::getConcernType)
                        .toList();

        // 최근 7일 근무 조회
        List<DutySchedule> recentSchedules =
                dutyScheduleRepository
                        .findAllByUserIdAndDateBetweenOrderByDateAsc(
                                userId,
                                date.minusDays(6),
                                date
                        );
        // 오늘 D/E/N/OFF/null
        ShiftType todayShiftType =
                findShiftType(
                        recentSchedules,
                        date
                );
        // 어제 D/E/N/OFF/null
        ShiftType previousShiftType =
                findShiftType(
                        recentSchedules,
                        date.minusDays(1)
                );
        // 오늘 포함 연속 근무 일수 - OFF/null에서 끊김
        int consecutiveWorkDays =
                calculateConsecutiveWorkDays(
                        recentSchedules,
                        date
                );

        // 오늘 포함 연속 N 일수 - N이 아닌 경우 끊김
        int consecutiveNightShiftDays =
                calculateConsecutiveNightShiftDays(
                        recentSchedules,
                        date
                );

        return new DailyPlanContext(
                date,

                todayShiftType,
                previousShiftType,

                consecutiveWorkDays,
                consecutiveNightShiftDays,

                user.getTargetSleepMinutes(),
                user.getMealPattern(),
                restActivities,

                userSkin.getSensitivityLevel(),
                userSkin.getSkinType(),
                skinConcerns,

                previousCheckin,
                previousRecoveryRoutineCompleted
        );
    }

    private ShiftType findShiftType(
            List<DutySchedule> schedules,
            LocalDate date
    ) {
        return schedules.stream()
                .filter(schedule ->
                        schedule.getDate().equals(date)
                )
                .map(DutySchedule::getShiftType)
                .findFirst()
                .orElse(null);
    }

    private int calculateConsecutiveWorkDays(
            List<DutySchedule> schedules,
            LocalDate date
    ) {
        int count = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate targetDate =
                    date.minusDays(i);

            ShiftType shiftType =
                    findShiftType(
                            schedules,
                            targetDate
                    );

            if (shiftType == null
                    || shiftType == ShiftType.OFF) {
                break;
            }

            count++;
        }

        return count;
    }

    private int calculateConsecutiveNightShiftDays(
            List<DutySchedule> schedules,
            LocalDate date
    ) {
        int count = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate targetDate =
                    date.minusDays(i);

            ShiftType shiftType =
                    findShiftType(
                            schedules,
                            targetDate
                    );

            if (shiftType != ShiftType.N) {
                break;
            }

            count++;
        }

        return count;
    }

    private PreviousCheckinContext toPreviousCheckinContext(
            Checkin checkin
    ) {
        return new PreviousCheckinContext(
                checkin.getFatigue(),
                checkin.getTightness(),
                checkin.getRedness(),
                checkin.getAnalyzedRedness(),
                checkin.getAnalyzedMoisture(),
                checkin.getAnalyzedOiliness(),
                checkin.getAnalyzedTrouble()
        );
    }
}
