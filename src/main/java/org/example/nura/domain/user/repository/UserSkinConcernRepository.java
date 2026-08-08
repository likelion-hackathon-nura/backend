package org.example.nura.domain.user.repository;

import org.example.nura.domain.user.entity.UserSkinConcern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSkinConcernRepository
        extends JpaRepository<UserSkinConcern, Long> {
}
