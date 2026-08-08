package org.example.nura.domain.user.repository;

import org.example.nura.domain.user.entity.UserRestActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRestActivityRepository
        extends JpaRepository<UserRestActivity, Long> {

    List<UserRestActivity> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
