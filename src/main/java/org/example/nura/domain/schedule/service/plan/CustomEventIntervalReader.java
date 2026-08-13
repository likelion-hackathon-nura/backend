// 오늘과 겹치는 기존 개인 일정 시간 읽어서 변환
// 충돌 검사할 때 재사용 아님 지우기, 지금은 그냥 레포지토리 그대로 갖다가 쓰는중
package org.example.nura.domain.schedule.service.plan;

import lombok.RequiredArgsConstructor;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.example.nura.domain.schedule.entity.CustomEvent;
import org.example.nura.domain.schedule.repository.CustomEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomEventIntervalReader {

    private final CustomEventRepository customEventRepository;

    public List<TimeInterval> read(
            Long userId,
            LocalDate date
    ) {
        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1).atStartOfDay();

        // 오늘과 겹치는 사용자 일정 조회
        return customEventRepository
                .findOverlappingEvents(
                        userId,
                        dayStart,
                        dayEnd
                )
                .stream()
                .map(event ->
                        toTodayInterval(
                                event,
                                dayStart,
                                dayEnd
                        )
                )
                .toList();
    }

    private TimeInterval toTodayInterval(
            CustomEvent event,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        LocalDateTime startAt =
                event.getStartAt().isBefore(dayStart)
                        ? dayStart
                        : event.getStartAt();

        LocalDateTime endAt =
                event.getEndAt().isAfter(dayEnd)
                        ? dayEnd
                        : event.getEndAt();

        return new TimeInterval(
                startAt,
                endAt
        );
    }
}
