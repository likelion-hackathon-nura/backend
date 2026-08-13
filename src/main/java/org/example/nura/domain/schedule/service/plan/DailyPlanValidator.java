// 전체 시간 잘 들어갔는지 검증
package org.example.nura.domain.schedule.service.plan;

import org.example.nura.domain.schedule.dto.plan.PlannedTimeBlock;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
public class DailyPlanValidator {

    private static final long MINUTES_PER_DAY = 24 * 60;

    public void validate(
            LocalDate date,
            List<PlannedTimeBlock> blocks
    ) {
        if (blocks == null || blocks.isEmpty()) {
            throw new IllegalStateException(
                    "하루 시간 설계 결과가 비어 있습니다."
            );
        }

        LocalDateTime dayStart =
                date.atStartOfDay();

        LocalDateTime dayEnd =
                date.plusDays(1)
                        .atStartOfDay();

        List<PlannedTimeBlock> sortedBlocks =
                blocks.stream()
                        .sorted(
                                Comparator.comparing(
                                        PlannedTimeBlock::startAt
                                )
                        )
                        .toList();

        validateBlockRanges(
                sortedBlocks,
                dayStart,
                dayEnd
        );

        validateContinuity(
                sortedBlocks,
                dayStart,
                dayEnd
        );

        validateTotalMinutes(
                sortedBlocks
        );
    }

    // 모든 블록이 오늘 범위 안에 있는지 확인
    private void validateBlockRanges(
            List<PlannedTimeBlock> blocks,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        for (PlannedTimeBlock block : blocks) {

            if (block.startAt() == null
                    || block.endAt() == null) {
                throw new IllegalStateException(
                        "시간 블록의 시작 또는 종료 시간이 없습니다."
                );
            }

            if (!block.endAt().isAfter(
                    block.startAt()
            )) {
                throw new IllegalStateException(
                        "시간 블록의 종료 시간은 시작 시간보다 이후여야 합니다."
                );
            }

            if (block.startAt().isBefore(dayStart)
                    || block.endAt().isAfter(dayEnd)) {
                throw new IllegalStateException(
                        "시간 블록이 오늘 범위를 벗어났습니다."
                );
            }
        }
    }

    // 블록 사이에 겹침이나 빈 시간이 없는지 확인
    private void validateContinuity(
            List<PlannedTimeBlock> blocks,
            LocalDateTime dayStart,
            LocalDateTime dayEnd
    ) {
        if (!blocks.get(0)
                .startAt()
                .equals(dayStart)) {
            throw new IllegalStateException(
                    "하루 시작 시간에 빈 구간이 있습니다."
            );
        }

        for (int i = 1; i < blocks.size(); i++) {

            PlannedTimeBlock previous =
                    blocks.get(i - 1);

            PlannedTimeBlock current =
                    blocks.get(i);

            if (!previous.endAt()
                    .equals(current.startAt())) {
                throw new IllegalStateException(
                        "시간 블록 사이에 겹침 또는 빈 구간이 있습니다."
                );
            }
        }

        if (!blocks.get(
                        blocks.size() - 1
                )
                .endAt()
                .equals(dayEnd)) {
            throw new IllegalStateException(
                    "하루 종료 시간까지 채워지지 않았습니다."
            );
        }
    }

    // 전체 시간이 정확히 1440분인지 확인
    private void validateTotalMinutes(
            List<PlannedTimeBlock> blocks
    ) {
        long totalMinutes =
                blocks.stream()
                        .mapToLong(block ->
                                Duration.between(
                                        block.startAt(),
                                        block.endAt()
                                ).toMinutes()
                        )
                        .sum();

        if (totalMinutes != MINUTES_PER_DAY) {
            throw new IllegalStateException(
                    "하루 시간 설계 합계가 1440분이 아닙니다."
            );
        }
    }
}
