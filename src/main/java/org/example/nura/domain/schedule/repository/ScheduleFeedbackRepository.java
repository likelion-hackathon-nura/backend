package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.ScheduleFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleFeedbackRepository
        extends JpaRepository<ScheduleFeedback, Long> {

    void deleteAllByUserId(Long userId);
}
