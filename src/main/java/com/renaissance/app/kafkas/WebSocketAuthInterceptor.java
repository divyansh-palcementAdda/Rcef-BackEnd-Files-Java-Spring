package com.renaissance.app.kafkas;

import com.renaissance.app.model.RefreshToken;
import com.renaissance.app.security.JwtProvider;
import com.renaissance.app.service.impl.RefreshTokenService;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * WebSocket Authentication Interceptor
 * - Validates JWT on STOMP CONNECT
 * - Checks token in DB (anti-reuse)
 * - Sets SecurityContext for @MessageMapping methods
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT rejected: Missing or invalid Authorization header");
                accessor.setLeaveMutable(true);
                return null; // Reject connection
            }

            String jwt = authHeader.substring(7).trim();

            try {
                UsernamePasswordAuthenticationToken auth = validateAndAuthenticate(jwt);
                if (auth != null) {
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.info("WebSocket CONNECT authenticated: {}", auth.getName());
                } else {
                    log.warn("WebSocket CONNECT rejected: Invalid or revoked token");
                    accessor.setLeaveMutable(true);
                    return null;
                }
            } catch (Exception e) {
                log.error("WebSocket auth failed: {}", e.getMessage());
                accessor.setLeaveMutable(true);
                return null;
            }
        }

        return message;
    }

    /**
     * Validate JWT + DB check + build auth token
     */
    private UsernamePasswordAuthenticationToken validateAndAuthenticate(String jwt) {
        // Step 1: Validate signature & expiry
        if (!jwtProvider.isTokenValid(jwt)) {
            log.debug("Invalid/expired JWT in WebSocket: {}", JwtProvider.maskToken(jwt));
            return null;
        }

        String username = jwtProvider.extractSubject(jwt);
        Long userId = jwtProvider.extractClaim(jwt, claims -> claims.get("userId", Long.class));
        String role = jwtProvider.extractClaim(jwt, claims -> claims.get("role", String.class));

        if (username == null || userId == null || role == null) {
            log.warn("JWT missing required claims: sub={}, userId={}, role={}", username, userId, role);
            return null;
        }

        // Step 2: Verify access token exists in DB and is not expired
        Optional<RefreshToken> sessionOpt = refreshTokenService.findByRefreshTokenForUser(userId, jwt);
        if (sessionOpt.isEmpty()) {
            log.warn("Access token not found in DB or expired for userId={}", userId);
            return null;
        }

        RefreshToken session = sessionOpt.get();
        if (session.isAccessExpired()) {
            log.info("WebSocket rejected: Access token expired for userId={}", userId);
            return null;
        }

        // Step 3: Build authenticated principal
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        User principal = new User(username, "", authorities);

        return new UsernamePasswordAuthenticationToken(
                principal, null, authorities
        );
    }
}