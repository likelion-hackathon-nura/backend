package org.example.nura.domain.schedule.service.overlay;

import org.example.nura.domain.schedule.entity.CustomEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomEventOverlayService {

    public OverlayResult overlay(
            List<CustomEvent> existingEvents,
            CustomEvent newEvent
    ) {
        List<CustomEvent> eventsToDelete =
                new ArrayList<>();

        List<CustomEvent> eventsToInsert =
                new ArrayList<>();

        Map<Long, CustomEvent> rightSplitEventsByOriginalId =
                new HashMap<>();

        for (CustomEvent existingEvent : existingEvents) {
            if (!overlaps(
                    existingEvent,
                    newEvent.getStartAt(),
                    newEvent.getEndAt()
            )) {
                continue;
            }

            LocalDateTime blockStart =
                    existingEvent.getStartAt();

            LocalDateTime blockEnd =
                    existingEvent.getEndAt();

            // 완전 덮음
            if (!newEvent.getStartAt().isAfter(blockStart)
                    && !newEvent.getEndAt().isBefore(blockEnd)) {
                eventsToDelete.add(existingEvent);
                continue;
            }

            // 가운데 split
            if (blockStart.isBefore(newEvent.getStartAt())
                    && blockEnd.isAfter(newEvent.getEndAt())) {
                existingEvent.update(
                        existingEvent.getCategory(),
                        existingEvent.getEventName(),
                        blockStart,
                        newEvent.getStartAt()
                );

                CustomEvent rightEvent =
                        CustomEvent.create(
                                existingEvent.getUser(),
                                existingEvent.getCategory(),
                                existingEvent.getEventName(),
                                newEvent.getEndAt(),
                                blockEnd
                        );

                eventsToInsert.add(rightEvent);
                rightSplitEventsByOriginalId.put(
                        existingEvent.getId(),
                        rightEvent
                );
                continue;
            }

            // 앞부분 trim
            if (blockStart.isBefore(newEvent.getStartAt())
                    && blockEnd.isAfter(newEvent.getStartAt())
                    && !blockEnd.isAfter(newEvent.getEndAt())) {
                existingEvent.update(
                        existingEvent.getCategory(),
                        existingEvent.getEventName(),
                        blockStart,
                        newEvent.getStartAt()
                );
                continue;
            }

            // 뒷부분 trim
            if (!blockStart.isBefore(newEvent.getStartAt())
                    && blockStart.isBefore(newEvent.getEndAt())
                    && blockEnd.isAfter(newEvent.getEndAt())) {
                existingEvent.update(
                        existingEvent.getCategory(),
                        existingEvent.getEventName(),
                        newEvent.getEndAt(),
                        blockEnd
                );
            }
        }

        return new OverlayResult(
                eventsToDelete,
                eventsToInsert,
                rightSplitEventsByOriginalId
        );
    }

    private boolean overlaps(
            CustomEvent event,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return event.getStartAt().isBefore(endAt)
                && event.getEndAt().isAfter(startAt);
    }

    public record OverlayResult(
            List<CustomEvent> eventsToDelete,
            List<CustomEvent> eventsToInsert,
            Map<Long, CustomEvent> rightSplitEventsByOriginalId
    ) {
    }
}
