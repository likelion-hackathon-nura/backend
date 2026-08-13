// 피부 회복 전용 시간대만 추림 - 퇴근 이후 배치, 복잡한 날짜 경계 처리
package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.entity.enums.ShiftType;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SkinRecoverySlotResolver {

    public List<AvailableSlotContext> resolve(
            LocalDate date,
            ShiftType todayShiftType,
            List<TimeInterval> workIntervals,
            List<AvailableSlotContext> availableSlots
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1)
                        .atStartOfDay();

        // 전날 N 근무가 오늘로 넘어온 구간
        TimeInterval previousNightTail =
                workIntervals.stream()
                        .filter(interval ->
                                interval.startAt()
                                        .equals(dayStart)
                        )
                        .findFirst()
                        .orElse(null);

        // 전날 N 근무에서 오늘 아침 퇴근한 경우
        if (previousNightTail != null) {
            return filterAfterWork(
                    availableSlots,
                    previousNightTail.endAt()
            );
        }

        // 오늘 D/E 근무
        if (todayShiftType == ShiftType.D
                || todayShiftType == ShiftType.E) {

            TimeInterval todayWork =
                    workIntervals.stream()
                            .filter(interval ->
                                    interval.startAt()
                                            .isAfter(dayStart)
                            )
                            .filter(interval ->
                                    interval.startAt()
                                            .isBefore(dayEnd)
                            )
                            .findFirst()
                            .orElse(null);

            if (todayWork != null) {
                return filterAfterWork(
                        availableSlots,
                        todayWork.endAt()
                );
            }
        }

        // 오늘 첫 N 근무 - 퇴근이 다음 날이므로 오늘 안에는 퇴근 이후 시간이 없음
        if (todayShiftType == ShiftType.N) {
            return List.of();
        }

        // OFF 또는 근무표 미등록
        return availableSlots;
    }

    private List<AvailableSlotContext> filterAfterWork(
            List<AvailableSlotContext> availableSlots,
            LocalDateTime workEnd
    ) {
        return availableSlots.stream()
                .filter(slot ->
                        slot.endAt()
                                .isAfter(workEnd)
                )
                .map(slot ->
                        trimAfterWork(
                                slot,
                                workEnd
                        )
                )
                .filter(slot ->
                        slot.durationMinutes() > 0
                )
                .toList();
    }

    private AvailableSlotContext trimAfterWork(
            AvailableSlotContext slot,
            LocalDateTime workEnd
    ) {
        LocalDateTime startAt =
                slot.startAt()
                        .isBefore(workEnd)
                        ? workEnd
                        : slot.startAt();

        long durationMinutes =
                Duration.between(
                        startAt,
                        slot.endAt()
                ).toMinutes();

        return new AvailableSlotContext(
                slot.slotId(),
                startAt,
                slot.endAt(),
                durationMinutes
        );
    }
}
