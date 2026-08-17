package org.example.nura.domain.skin.repository;

import org.example.nura.domain.skin.entity.SkinRoutineFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkinRoutineFeedbackRepository extends JpaRepository<SkinRoutineFeedback, Long> {
    // 유저의 최신 피드백 3건 가져오기
    List<SkinRoutineFeedback> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);
}