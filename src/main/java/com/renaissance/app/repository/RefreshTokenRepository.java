package com.renaissance.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.renaissance.app.model.RefreshToken;
import com.renaissance.app.model.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUser(User user);
    void deleteByUser_UserId(Long userId);
}

