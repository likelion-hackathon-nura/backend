package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.ScheduleFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ScheduleFeedbackRepository
        extends JpaRepository<ScheduleFeedback, Long> {

    Optional<ScheduleFeedback> findByUserIdAndFeedbackDate(
            Long userId,
            LocalDate feedbackDate
    );

    boolean existsByUserIdAndFeedbackDate(
            Long userId,
            LocalDate feedbackDate
    );

    void deleteAllByUserId(Long userId);
}
