package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.TimeBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimeBlockRepository
        extends JpaRepository<TimeBlock, Long> {

    List<TimeBlock> findAllByAllocationIdOrderByStartAtAsc(
            Long allocationId
    );

    void deleteAllByAllocationId(
            Long allocationId
    );
}
