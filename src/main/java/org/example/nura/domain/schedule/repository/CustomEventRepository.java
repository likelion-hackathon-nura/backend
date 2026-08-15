package org.example.nura.domain.schedule.repository;

import org.example.nura.domain.schedule.entity.CustomEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomEventRepository
        extends JpaRepository<CustomEvent, Long> {

    @Query("""
            select event
            from CustomEvent event
            where event.user.id = :userId
              and event.startAt < :endAt
              and event.endAt > :startAt
            order by event.startAt asc
            """)
    List<CustomEvent> findOverlappingEvents(
            @Param("userId") Long userId,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );

    void deleteAllByUserId(Long userId);
}
