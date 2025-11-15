package com.renaissance.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Secure JWT Configuration
 * - Loaded from application.yml / application-{profile}.yml
 * - Validates secret key length (>= 256 bits)
 * - Defaults: 15 min access, 7 days refresh
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * Secret key for signing JWTs.
     * Must be at least 256 bits (32 bytes) when UTF-8 encoded.
     * Recommended: Base64-encoded 32+ random bytes.
     */
    @NotBlank(message = "JWT secret must not be blank")
    private String secret;

    /**
     * Access token validity in milliseconds.
     * Default: 15 minutes (900,000 ms)
     */
    @Positive(message = "Access token expiration must be positive")
    private long accessTokenExpirationMs = 900_000L; // 15 min

    /**
     * Refresh token validity in milliseconds.
     * Default: 7 days (604,800,000 ms)
     */
    @Positive(message = "Refresh token expiration must be positive")
    private long refreshTokenExpirationMs = 604_800_000L; // 7 days

    /* --------------------------------------------------------------------- */
    /* VALIDATION HOOK (called after properties are bound) */
    /* --------------------------------------------------------------------- */
    public void validate() {
        byte[] keyBytes = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 256 bits (32 bytes). " +
                "Current length: " + keyBytes.length + " bytes. " +
                "Use `openssl rand -base64 32` to generate a secure key."
            );
        }
    }

    /* --------------------------------------------------------------------- */
    /* GETTERS (for backward compatibility if needed) */
    /* --------------------------------------------------------------------- */
    public String getJwtSecret() {
        return secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}