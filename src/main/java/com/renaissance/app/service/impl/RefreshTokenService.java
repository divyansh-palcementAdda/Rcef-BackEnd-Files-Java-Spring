package com.renaissance.app.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.model.RefreshToken;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.repository.RefreshTokenRepository;
import com.renaissance.app.security.JwtProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	private final JwtProvider jwtProvider;

	@Value("${jwt.refresh.expiration-ms:604800000}") // 7 days default
	private long refreshTokenDurationMs;

	@Value("${jwt.access.expiration-ms:900000}") // 15 min default
	private long accessTokenDurationMs;

	/* --------------------------------------------------------------------- */
	/* CREATE NEW SESSION (LOGIN) */
	/* --------------------------------------------------------------------- */
	@Transactional
	public RefreshToken createRefreshToken(User user, String accessToken, String clientIp, String deviceInfo)
			throws AccessDeniedException {
		validateUser(user);

		// Revoke any existing session from same IP + device (optional: prevent
		// duplicates)
		// refreshTokenRepository.deleteByUserAndClientIpAndDeviceInfo(user, clientIp,
		// deviceInfo);

		RefreshToken token = RefreshToken.builder().user(user).refreshToken(UUID.randomUUID().toString())
				.accessToken(accessToken).clientIp(clientIp).deviceInfo(deviceInfo)
				.expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
				.accessTokenExpiry(Instant.now().plusMillis(accessTokenDurationMs)).lastUsed(Instant.now()).build();

		RefreshToken saved = refreshTokenRepository.save(token);
		log.info("New refresh token created for userId={}, ip={}", user.getUserId(), clientIp);
		return saved;
	}

	/* --------------------------------------------------------------------- */
	/* FIND BY REFRESH TOKEN */
	/* --------------------------------------------------------------------- */
	public Optional<RefreshToken> findByRefreshToken(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		return refreshTokenRepository.findByRefreshToken(token);
	}
	/**
	 * Find active session by userId and access token
	 */
	public Optional<RefreshToken> findByRefreshTokenForUser(Long userId, String accessToken) {
	    return refreshTokenRepository.findByUser_UserIdAndAccessToken(userId, accessToken)
	            .filter(rt -> !rt.isAccessExpired());
	}

	/* --------------------------------------------------------------------- */
	/* VERIFY & REFRESH (ROTATE ACCESS TOKEN) */
	/* --------------------------------------------------------------------- */
	@Transactional
	public RefreshToken verifyAndRefresh(RefreshToken token) throws AccessDeniedException, BadRequestException {
		if (token == null) {
			throw new BadRequestException("Refresh token is required");
		}

		if (token.isRefreshExpired()) {
			refreshTokenRepository.delete(token);
			log.warn("Expired refresh token used: {}", maskToken(token.getRefreshToken()));
			throw new AccessDeniedException("Refresh token expired. Please login again.");
		}

		// Update last used
		token.setLastUsed(Instant.now());
		if (token.isAccessExpired()) {
			User user = token.getUser();
			String newAccessToken = jwtProvider.generateAccessToken(
			        user.getEmail(),
			        Map.of(
			                "userId", user.getUserId(),
			                "role",   user.getRole().name()
			        )
			);
			token.setAccessToken(newAccessToken);
			token.setAccessTokenExpiry(Instant.now().plusMillis(accessTokenDurationMs));
			log.info("Access token rotated for userId={}, ip={}", user.getUserId(), token.getClientIp());
		}

		return refreshTokenRepository.save(token);
	}

	/* --------------------------------------------------------------------- */
	/* LOGOUT - PER DEVICE */
	/* --------------------------------------------------------------------- */
	@Transactional
	public void revokeByRefreshToken(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			log.warn("Attempt to revoke null/blank refresh token");
			return;
		}

		refreshTokenRepository.findByRefreshToken(refreshToken).ifPresentOrElse(rt -> {
			log.info("Revoking refresh token for userId={}, ip={}", rt.getUser().getUserId(), rt.getClientIp());
			refreshTokenRepository.delete(rt);
		}, () -> log.debug("Refresh token not found (already revoked): {}", maskToken(refreshToken)));
	}

	/* --------------------------------------------------------------------- */
	/* LOGOUT - ALL DEVICES (GLOBAL) */
	/* --------------------------------------------------------------------- */
	@Transactional
	public void revokeAllByUserId(Long userId) {
		if (userId == null)
			return;

		int deleted = refreshTokenRepository.deleteByUser_UserId(userId);
		log.info("Revoked {} refresh token(s) for userId={}", deleted, userId);
	}

	/* --------------------------------------------------------------------- */
	/* GET ACTIVE SESSIONS (FOR UI) */
	/* --------------------------------------------------------------------- */
	@Transactional(readOnly = true)
	public List<RefreshToken> getActiveSessions(Long userId) {
		return refreshTokenRepository.findByUser_UserId(userId);
	}

	/* --------------------------------------------------------------------- */
	/* UTILITIES */
	/* --------------------------------------------------------------------- */
	private void validateUser(User user) throws AccessDeniedException {
		if (user == null || user.getUserId() == null) {
			throw new IllegalArgumentException("Valid user is required");
		}
		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new AccessDeniedException("User account is inactive");
		}
	}

	private String maskToken(String token) {
		if (token == null || token.length() < 8)
			return "****";
		return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
	}
}