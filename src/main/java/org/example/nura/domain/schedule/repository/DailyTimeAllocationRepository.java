package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.DailyTimeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTimeAllocationRepository
        extends JpaRepository<DailyTimeAllocation, Long> {

    Optional<DailyTimeAllocation> findByUserIdAndDate(
            Long userId,
            LocalDate date
    );

    List<DailyTimeAllocation> findAllByUserId(Long userId);

    List<DailyTimeAllocation> findAllByUserIdAndDateBetweenOrderByDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate
    );
    boolean existsByUserIdAndDate(
            Long userId,
            LocalDate date
    );


    void deleteAllByUserId(Long userId);
}
