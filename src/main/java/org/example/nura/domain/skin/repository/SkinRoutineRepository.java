package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.SkinRoutine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SkinRoutineRepository extends JpaRepository<SkinRoutine, Long> {

	@Query("SELECT sr FROM SkinRoutine sr " +
			"JOIN FETCH sr.checkin c " +
			"WHERE c.user.id = :userId AND c.date = :date")
	Optional<SkinRoutine> findByCheckinUserIdAndCheckinDate(
			@Param("userId") Long userId,
			@Param("date") LocalDate date
	);

	// 주간 루틴 목록 조회 쿼리
	@Query("SELECT sr FROM SkinRoutine sr " +
			"JOIN FETCH sr.checkin c " +
			"WHERE c.user.id = :userId AND c.date BETWEEN :startDate AND :endDate")
	List<SkinRoutine> findAllByUserIdAndDateBetween(
			@Param("userId") Long userId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate
	);

	void deleteAllByCheckinUserId(Long userId);
}