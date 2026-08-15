package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.SkinRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface SkinRoutineRepository extends JpaRepository<SkinRoutine, Long> {

	@Query("SELECT sr FROM SkinRoutine sr " +
			"JOIN FETCH sr.checkin c " +
			"WHERE c.user.id = :userId AND c.date = :date")
	Optional<SkinRoutine> findByCheckinUserIdAndCheckinDate(
			@Param("userId") Long userId,
			@Param("date") LocalDate date
	);

	void deleteAllByCheckinUserId(Long userId);
}