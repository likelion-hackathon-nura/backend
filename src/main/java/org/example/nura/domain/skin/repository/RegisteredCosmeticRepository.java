package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegisteredCosmeticRepository extends JpaRepository<RegisteredCosmetic, Long> {
    List<RegisteredCosmetic> findAllByUserId(Long userId);
}

