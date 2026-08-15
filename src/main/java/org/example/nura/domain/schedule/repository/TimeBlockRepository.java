package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.TimeBlock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeBlockRepository
        extends JpaRepository<TimeBlock, Long> {

    List<TimeBlock> findAllByAllocationIdOrderByStartAtAsc(
            Long allocationId
    );

    void deleteAllByAllocationId(
            Long allocationId
    );

    @Modifying
    @Query("""
            delete from TimeBlock tb
            where tb.allocation.user.id = :userId
            """)
    void deleteAllByAllocationUserId(
            @Param("userId") Long userId
    );
}
