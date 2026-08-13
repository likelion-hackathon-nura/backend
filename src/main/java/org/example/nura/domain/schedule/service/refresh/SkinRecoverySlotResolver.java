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

        // 1. 오늘 D/E 근무가 있으면 오늘 퇴근 이후
        if (todayShiftType == ShiftType.D
                || todayShiftType == ShiftType.E) {

            TimeInterval todayWork =
                    workIntervals.stream()
                            .filter(interval ->
                                    interval.startAt()
                                            .isAfter(dayStart)
                                            && interval.startAt()
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

        // 2. 전날 N 근무가 오늘 아침까지 이어졌다면 -> 해당 근무 퇴근 이후에만 피부 회복 가능
        if (previousNightTail != null) {
            return filterAfterWork(
                    availableSlots,
                    previousNightTail.endAt()
            );
        }

        // 3. 오늘 첫 N 근무 -> 오늘 사용 가능한 빈 시간에 피부 회복 가능
        if (todayShiftType == ShiftType.N) {
            return availableSlots;
        }

        // 4. OFF 또는 근무표 미등록
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
