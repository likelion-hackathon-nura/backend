package org.example.nura.domain.cosmetics.repository;

import org.example.nura.domain.cosmetics.entity.RegisteredCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegisteredCosmeticRepository extends JpaRepository<RegisteredCosmetic, Long> {

    List<RegisteredCosmetic> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<RegisteredCosmetic> findByIdAndUserId(Long id, Long userId);
    List<RegisteredCosmetic> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RegisteredCosmetic> findByUserIdAndCosmeticNameContainingIgnoreCaseOrderByCreatedAtDesc(Long userId, String cosmeticName);

    void deleteAllByUserId(Long userId);
}