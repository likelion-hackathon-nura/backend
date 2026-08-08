package org.example.nura.domain.user.repository;

import org.example.nura.domain.user.entity.UserRestActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRestActivityRepository
        extends JpaRepository<UserRestActivity, Long> {
}
