package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.SkinRoutine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SkinRoutineRepository extends JpaRepository<SkinRoutine, Long> {
	Optional<SkinRoutine> findByCheckinUserIdAndCheckinDate(
			Long userId,
			LocalDate date
	);
}

