package com.renaissance.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.renaissance.app.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByRefreshToken(String token);

	void deleteByRefreshToken(String token);

	Optional<RefreshToken> findByUser_UserIdAndAccessToken(Long userId, String accessToken);

	List<RefreshToken> findByUser_UserId(Long userId);

	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.user.userId = :userId")
	int deleteByUser_UserId(Long userId);

	// Optional: clean up expired tokens
	@Modifying
	@Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
	int deleteExpiredTokens();
}
