package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisteredCosmeticRepository extends JpaRepository<RegisteredCosmetic, Long> {

    List<RegisteredCosmetic> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<RegisteredCosmetic> findByIdAndUserId(Long id, Long userId);

    void deleteAllByUserId(Long userId);
}