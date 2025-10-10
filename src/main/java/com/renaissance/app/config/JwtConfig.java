package com.renaissance.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for JWT properties, loaded from application.yml /
 * application.properties
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

	@Value("${jwt.secret}")
	private String jwtSecret;

	@Value("${jwt.expiration:3600000}") // Default to 1 hour if not specified
	private long jwtExpirationMs;

	@Value("${jwt.refresh.expiration:604800000}") // Default to 7 days for refresh token
	private long jwtRefreshExpirationMs;

	public String getJwtSecret() {
		return jwtSecret;
	}

	public long getJwtExpiration() {
		return jwtExpirationMs;
	}

	public long getJwtRefreshExpirationMs() {
		return jwtRefreshExpirationMs;
	}
}
