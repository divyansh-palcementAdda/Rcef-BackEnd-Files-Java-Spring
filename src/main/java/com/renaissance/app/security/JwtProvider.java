package com.renaissance.app.security;

import com.renaissance.app.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * Secure JWT Provider for Access & Refresh Tokens
 * - HS512 with strong key (min 256-bit)
 * - Configurable expiry
 * - Secure logging (no token in logs)
 * - Thread-safe & stateless
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtConfig jwtConfig;

    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtConfig.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) { // 256 bits = 32 bytes
            log.error("JWT secret key must be at least 256 bits (32 bytes). Current: {} bytes", keyBytes.length);
            throw new IllegalStateException("JWT secret key is too weak");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /* --------------------------------------------------------------------- */
    /* GENERATE ACCESS TOKEN */
    /* --------------------------------------------------------------------- */
    public String generateAccessToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, jwtConfig.getAccessTokenExpirationMs());
    }

    /* --------------------------------------------------------------------- */
    /* GENERATE REFRESH TOKEN */
    /* --------------------------------------------------------------------- */
    public String generateRefreshToken(String subject, Map<String, Object> claims) {
        return buildToken(subject, claims, jwtConfig.getRefreshTokenExpirationMs());
    }

    /* --------------------------------------------------------------------- */
    /* BUILD TOKEN (INTERNAL) */
    /* --------------------------------------------------------------------- */
    private String buildToken(String subject, Map<String, Object> claims, long expirationMs) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Subject (username/email) is required");
        }
        if (expirationMs <= 0) {
            throw new IllegalArgumentException("Token expiration must be positive");
        }

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSecretKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /* --------------------------------------------------------------------- */
    /* EXTRACT SUBJECT (USERNAME / EMAIL) */
    /* --------------------------------------------------------------------- */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /* --------------------------------------------------------------------- */
    /* EXTRACT ANY CLAIM */
    /* --------------------------------------------------------------------- */
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }

    /* --------------------------------------------------------------------- */
    /* EXTRACT ALL CLAIMS (with full exception handling) */
    /* --------------------------------------------------------------------- */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired at {}", e.getClaims().getExpiration());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("JWT format unsupported");
            throw new JwtException("Unsupported JWT", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token");
            throw new JwtException("Malformed JWT", e);
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature");
            throw new JwtException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null");
            throw new JwtException("Empty JWT", e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* VALIDATE TOKEN */
    /* --------------------------------------------------------------------- */
    public boolean isTokenValid(String token, String expectedSubject) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String subject = extractSubject(token);
            return subject.equals(expectedSubject) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.debug("Invalid token during validation: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractAllClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true; // expired or invalid
        }
    }

    /* --------------------------------------------------------------------- */
    /* UTILITIES */
    /* --------------------------------------------------------------------- */
    public long getAccessTokenExpiry() {
        return jwtConfig.getAccessTokenExpirationMs();
    }

    public long getRefreshTokenExpiry() {
        return jwtConfig.getRefreshTokenExpirationMs();
    }
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    /**
     * Mask token for logging (show first 4 + last 4 chars)
     */
    public static String maskToken(String token) {
        if (token == null || token.length() <= 8) return "****";
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}