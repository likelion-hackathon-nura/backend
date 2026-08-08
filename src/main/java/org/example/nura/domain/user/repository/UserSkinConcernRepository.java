package org.example.nura.domain.user.repository;

import org.example.nura.domain.user.entity.UserSkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserSkinConcernRepository
        extends JpaRepository<UserSkinConcern, Long> {

    List<UserSkinConcern> findAllByUserSkinId(Long userSkinId);

    void deleteAllByUserSkinId(Long userSkinId);
}