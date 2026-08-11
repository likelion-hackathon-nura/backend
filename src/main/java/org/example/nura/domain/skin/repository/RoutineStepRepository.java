package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.RoutineStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoutineStepRepository extends JpaRepository<RoutineStep, Long> {
    List<RoutineStep> findAllByRoutineIdOrderByStepOrderAsc(Long routineId);

    void deleteAllByRoutineId(Long routineId);
}

