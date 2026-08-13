// openai용 슬롯 변환
package org.example.nura.domain.schedule.service.refresh;

import org.example.nura.domain.schedule.dto.context.AvailableSlotContext;
import org.example.nura.domain.schedule.dto.context.TimeInterval;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class AvailableSlotMapper {

    public List<AvailableSlotContext> map(
            List<TimeInterval> intervals
    ) {
        List<AvailableSlotContext> slots =
                new ArrayList<>();

        for (int i = 0; i < intervals.size(); i++) {

            TimeInterval interval =
                    intervals.get(i);

            long durationMinutes =
                    ChronoUnit.MINUTES.between(
                            interval.startAt(),
                            interval.endAt()
                    );

            slots.add(
                    new AvailableSlotContext(
                            "S" + (i + 1),
                            interval.startAt(),
                            interval.endAt(),
                            durationMinutes
                    )
            );
        }

        return slots;
    }
}
