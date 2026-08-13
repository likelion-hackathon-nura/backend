package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.RegisteredCosmetic;
import org.example.nura.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegisteredCosmeticRepository extends JpaRepository<RegisteredCosmetic, Long> {

    // 특정 유저가 등록한 모든 화장품 목록 조회
    List<RegisteredCosmetic> findByUserId(Long userId);

    // 특정 유저의 화장품 존재 여부 확인
    boolean existsByUserId(Long userId);
}