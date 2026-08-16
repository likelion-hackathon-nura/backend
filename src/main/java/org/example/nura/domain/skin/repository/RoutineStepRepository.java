package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.RoutineStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoutineStepRepository extends JpaRepository<RoutineStep, Long> {
    List<RoutineStep> findAllByRoutineIdOrderByStepOrderAsc(Long routineId);

    void deleteAllByRoutineId(Long routineId);

    void deleteAllByRoutineCheckinUserId(Long userId);

    @Modifying
    @Query("UPDATE RoutineStep rs SET rs.registeredCosmetic = null WHERE rs.registeredCosmetic.id = :cosmeticId")
    void bulkSetCosmeticNull(@Param("cosmeticId") Long cosmeticId);

}
