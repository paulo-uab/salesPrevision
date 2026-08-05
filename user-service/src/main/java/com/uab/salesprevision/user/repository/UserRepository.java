package com.uab.salesprevision.user.repository;

import com.uab.salesprevision.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndActiveTrue(String username);
    boolean existsByUsername(String username);
}
