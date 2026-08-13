package org.example.nura.domain.home.dto.response;

import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;

import java.time.LocalDateTime;

public record HomeBlockResponse(
        Long blockId,
        TimeCategory category,
        String label,
        LocalDateTime startAt,
        LocalDateTime endAt,
        TimeBlockSource source,
        boolean deletable,
        Boolean completed,
        Long eventId
) {

    public static HomeBlockResponse from(
            TimeBlock block
    ) {
        Long eventId =
                block.getCustomEvent() == null
                        ? null
                        : block.getCustomEvent().getId();

        return new HomeBlockResponse(
                block.getId(),
                block.getCategory(),
                block.getLabel(),
                block.getStartAt(),
                block.getEndAt(),
                block.getSource(),
                block.getSource() == TimeBlockSource.MANUAL,
                block.getCompleted(),
                eventId
        );
    }
}
