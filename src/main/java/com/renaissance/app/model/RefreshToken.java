package com.renaissance.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One row = one active login session (one device / browser / phone).
 * - refreshToken  -> long-lived token used to obtain a new access token
 * - accessToken   -> short-lived JWT that the client sends with every request
 * - clientIp      -> IP address from which the login request originated
 * - deviceInfo    -> optional free-form field (e.g. "Chrome 128 on Windows 11")
 * - lastUsed      -> updated each time the refresh token is used
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_token", columnList = "token", unique = true),
        @Index(name = "idx_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* --------------------------------------------------------------------- *
     *  One RefreshToken belongs to exactly one User (but a User can have   *
     *  many RefreshTokens – one per active session).                        *
     * --------------------------------------------------------------------- */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    /* --------------------------------------------------------------------- *
     *  Refresh token – unique, never changes for the lifetime of the row.  *
     * --------------------------------------------------------------------- */
    @Column(nullable = false, unique = true, length = 512)
    private String refreshToken;

    /* --------------------------------------------------------------------- *
     *  Current access token (JWT) – updated on every token rotation.        *
     * --------------------------------------------------------------------- */
    @Column(nullable = false, length = 1024)
    private String accessToken;

    /* --------------------------------------------------------------------- *
     *  IP address of the client that performed the initial login.          *
     * --------------------------------------------------------------------- */
    @Column(name = "client_ip", nullable = false, length = 45) // IPv4 or IPv6
    private String clientIp;

    /* --------------------------------------------------------------------- *
     *  Optional free-form device identifier (User-Agent, app version …)    *
     * --------------------------------------------------------------------- */
    @Column(name = "device_info", length = 512)
    private String deviceInfo;

    /* --------------------------------------------------------------------- *
     *  When the *refresh* token expires.                                   *
     * --------------------------------------------------------------------- */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /* --------------------------------------------------------------------- *
     *  When the access token expires (usually a few minutes).              *
     * --------------------------------------------------------------------- */
    @Column(name = "access_expiry", nullable = false)
    private Instant accessTokenExpiry;

    /* --------------------------------------------------------------------- *
     *  Timestamp of the last time the refresh token was used to get a new  *
     *  access token – useful for “last seen” UI and revocation detection. *
     * --------------------------------------------------------------------- */
    @Column(name = "last_used")
    private Instant lastUsed;

    /* --------------------------------------------------------------------- *
     *  Helper: is the refresh token still valid?                           *
     * --------------------------------------------------------------------- */
    @Transient
    public boolean isRefreshExpired() {
        return Instant.now().isAfter(expiryDate);
    }

    @Transient
    public boolean isAccessExpired() {
        return Instant.now().isAfter(accessTokenExpiry);
    }
}