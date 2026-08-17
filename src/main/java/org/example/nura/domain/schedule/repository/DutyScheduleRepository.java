package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.DutySchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DutyScheduleRepository
        extends JpaRepository<DutySchedule, Long> {

    Optional<DutySchedule> findByUserIdAndDate(
            Long userId,
            LocalDate date
    );

    List<DutySchedule> findAllByUserIdAndDateBetweenOrderByDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );


    boolean existsByUserIdAndDate(
            Long userId,
            LocalDate date
    );

    void deleteAllByUserId(Long userId);
}
