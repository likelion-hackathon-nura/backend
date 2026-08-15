package org.example.nura.domain.schedule.service.overlay;

import org.example.nura.domain.schedule.entity.CustomEvent;
import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.example.nura.domain.schedule.entity.TimeBlock;
import org.example.nura.domain.schedule.entity.enums.TimeBlockSource;
import org.example.nura.domain.schedule.entity.enums.TimeCategory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class TimeBlockOverlayService {

    public OverlayResult overlay(
            DailyTimeAllocation allocation,
            List<TimeBlock> existingBlocks,
            TimeCategory category,
            String label,
            LocalDateTime startAt,
            LocalDateTime endAt,
            CustomEvent customEvent,
            Map<Long, CustomEvent> rightSplitEventsByOriginalId
    ) {
        return overlay(
                allocation,
                existingBlocks,
                category,
                label,
                startAt,
                endAt,
                customEvent,
                rightSplitEventsByOriginalId,
                TimeBlockSource.MANUAL
        );
    }

    public OverlayResult overlay(
            DailyTimeAllocation allocation,
            List<TimeBlock> existingBlocks,
            TimeCategory category,
            String label,
            LocalDateTime startAt,
            LocalDateTime endAt,
            CustomEvent customEvent,
            Map<Long, CustomEvent> rightSplitEventsByOriginalId,
            TimeBlockSource newSource
    ) {
        TimeBlock newBlock =
                TimeBlock.create(
                        allocation,
                        customEvent,
                        category,
                        label,
                        startAt,
                        endAt,
                        newSource,
                        null
                );

        List<TimeBlock> blocksToDelete =
                new ArrayList<>();

        List<TimeBlock> blocksToInsert =
                new ArrayList<>();

        List<TimeBlock> finalBlocks =
                new ArrayList<>();

        for (TimeBlock existingBlock : existingBlocks) {
            OverlayDecision decision =
                    overlayOne(
                            existingBlock,
                            startAt,
                            endAt,
                            rightSplitEventsByOriginalId
                    );

            blocksToDelete.addAll(
                    decision.blocksToDelete()
            );
            blocksToInsert.addAll(
                    decision.blocksToInsert()
            );
            finalBlocks.addAll(
                    decision.finalBlocks()
            );
        }

        finalBlocks.add(newBlock);
        finalBlocks.sort(
                Comparator.comparing(
                        TimeBlock::getStartAt
                )
        );

        blocksToInsert.add(newBlock);

        return new OverlayResult(
                finalBlocks,
                blocksToDelete,
                blocksToInsert
        );
    }

    public boolean overlaps(
            TimeBlock block,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        return block.getStartAt().isBefore(endAt)
                && block.getEndAt().isAfter(startAt);
    }

    private OverlayDecision overlayOne(
            TimeBlock existingBlock,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Map<Long, CustomEvent> rightSplitEventsByOriginalId
    ) {
        if (!overlaps(
                existingBlock,
                startAt,
                endAt
        )) {
            return OverlayDecision.keep(existingBlock);
        }

        LocalDateTime blockStart =
                existingBlock.getStartAt();

        LocalDateTime blockEnd =
                existingBlock.getEndAt();

        // 1. 신규가 기존을 완전히 덮음 delete
        if (!startAt.isAfter(blockStart)
                && !endAt.isBefore(blockEnd)) {
            return OverlayDecision.delete(existingBlock);
        }

        // 2. 기존 블록 가운데를 신규가 침범 split
        if (blockStart.isBefore(startAt)
                && blockEnd.isAfter(endAt)) {
            TimeBlock left =
                    copyWithTime(
                            existingBlock,
                            blockStart,
                            startAt,
                            rightSplitEventsByOriginalId
                    );

            TimeBlock right =
                    copyWithTime(
                            existingBlock,
                            endAt,
                            blockEnd,
                            rightSplitEventsByOriginalId
                    );

            return OverlayDecision.split(
                    existingBlock,
                    left,
                    right
            );
        }

        // 3. 기존 앞부분을 신규가 덮음 trim
        if (blockStart.isBefore(startAt)
                && blockEnd.isAfter(startAt)
                && !blockEnd.isAfter(endAt)) {
            existingBlock.updateTime(
                    blockStart,
                    startAt
            );

            return OverlayDecision.update(existingBlock);
        }

        // 4. 기존 뒷부분을 신규가 덮음 trim
        if (!blockStart.isBefore(startAt)
                && blockStart.isBefore(endAt)
                && blockEnd.isAfter(endAt)) {
            CustomEvent rightCustomEvent =
                    existingBlock.getCustomEvent();

            if (rightCustomEvent != null
                    && rightSplitEventsByOriginalId != null) {
                CustomEvent mappedRightEvent =
                        rightSplitEventsByOriginalId.get(
                                rightCustomEvent.getId()
                        );

                if (mappedRightEvent != null) {
                    rightCustomEvent = mappedRightEvent;
                }
            }

            existingBlock.updateTime(
                    endAt,
                    blockEnd
            );

            if (rightCustomEvent != null) {
                existingBlock.updateCustomEvent(
                        rightCustomEvent
                );
            }

            return OverlayDecision.update(existingBlock);
        }

        return OverlayDecision.keep(existingBlock);
    }

    private TimeBlock copyWithTime(
            TimeBlock source,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Map<Long, CustomEvent> rightSplitEventsByOriginalId
    ) {
        CustomEvent customEvent =
                source.getCustomEvent();

        if (customEvent != null
                && rightSplitEventsByOriginalId != null) {
            CustomEvent mappedRightEvent =
                    rightSplitEventsByOriginalId.get(
                            customEvent.getId()
                    );

            if (mappedRightEvent != null) {
                customEvent = mappedRightEvent;
            }
        }

        return TimeBlock.create(
                source.getAllocation(),
                customEvent,
                source.getCategory(),
                source.getLabel(),
                startAt,
                endAt,
                source.getSource(),
                source.getCompleted()
        );
    }

    public record OverlayResult(
            List<TimeBlock> finalBlocks,
            List<TimeBlock> blocksToDelete,
            List<TimeBlock> blocksToInsert
    ) {
    }

    private record OverlayDecision(
            List<TimeBlock> finalBlocks,
            List<TimeBlock> blocksToDelete,
            List<TimeBlock> blocksToInsert
    ) {
        static OverlayDecision keep(
                TimeBlock block
        ) {
            return new OverlayDecision(
                    List.of(block),
                    List.of(),
                    List.of()
            );
        }

        static OverlayDecision update(
                TimeBlock block
        ) {
            return new OverlayDecision(
                    List.of(block),
                    List.of(),
                    List.of()
            );
        }

        static OverlayDecision delete(
                TimeBlock block
        ) {
            return new OverlayDecision(
                    List.of(),
                    List.of(block),
                    List.of()
            );
        }

        static OverlayDecision split(
                TimeBlock original,
                TimeBlock left,
                TimeBlock right
        ) {
            List<TimeBlock> finalBlocks =
                    new ArrayList<>();
            finalBlocks.add(left);
            finalBlocks.add(right);

            return new OverlayDecision(
                    finalBlocks,
                    List.of(original),
                    List.of(left, right)
            );
        }
    }
}
