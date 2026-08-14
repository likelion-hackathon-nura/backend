package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {
    boolean existsByUserIdAndDate(
            Long userId,
            LocalDate date
    );

    Optional<Checkin> findByUserIdAndDate(
            Long userId,
            LocalDate date
    );
}

