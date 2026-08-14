package org.example.nura.domain.user.repository;

import org.example.nura.domain.user.entity.UserSkin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSkinRepository
        extends JpaRepository<UserSkin, Long> {

    Optional<UserSkin> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
