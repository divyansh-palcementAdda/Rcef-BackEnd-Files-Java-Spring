package com.renaissance.app.security;

import com.renaissance.app.model.RefreshToken;
import com.renaissance.app.service.impl.RefreshTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * JWT Authentication Filter
 * - Validates JWT signature & expiry
 * - Cross-checks access token with DB (anti-reuse)
 * - Sets Spring Security context
 * - Returns 401 for invalid/expired/revoked tokens
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshTokenService refreshTokenService;

    // Public endpoints (no JWT required)
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/register",
            "/api/auth/verify-otp",
            "/api/auth/resend-otp",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/webjars",
            "/swagger-resources"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Skip JWT validation for public endpoints
        if (isPublicPath(path)) {
            log.debug("Skipping JWT check for public endpoint: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromRequest(request);

        if (jwt == null || jwt.isBlank()) {
            log.debug("No JWT token found in request for path: {}", path);
            sendUnauthorized(response, "Missing JWT token");
            return;
        }

        try {
            // Step 1: Validate JWT signature & expiry
            if (!jwtProvider.isTokenValid(jwt)) {
                log.warn("Invalid or expired JWT token: {}", JwtProvider.maskToken(jwt));
                sendUnauthorized(response, "Invalid or expired token");
                return;
            }

            String username = jwtProvider.extractSubject(jwt);
            Long userId = jwtProvider.extractClaim(jwt, claims -> claims.get("userId", Long.class));

            if (username == null || userId == null) {
                log.warn("JWT missing subject or userId claim");
                sendUnauthorized(response, "Invalid token payload");
                return;
            }

            // Step 2: Verify token exists in DB and is not expired
            if (!isTokenInDatabaseAndValid(jwt, userId)) {
                log.warn("Access token not found in DB or expired for userId={}", userId);
                sendUnauthorized(response, "Token revoked or expired");
                return;
            }

            // Step 3: Load user and set authentication
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authentication set for user: {} | IP: {}", username, getClientIp(request));
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            sendUnauthorized(response, "Authentication failed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /* --------------------------------------------------------------------- */
    /* Extract JWT from Authorization: Bearer <token> */
    /* --------------------------------------------------------------------- */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    /* --------------------------------------------------------------------- */
    /* Check if token exists in DB and access token is not expired */
    /* --------------------------------------------------------------------- */
    private boolean isTokenInDatabaseAndValid(String accessToken, Long userId) {
        return refreshTokenService.findByRefreshTokenForUser(userId, accessToken)
                .map(rt -> {
                    boolean valid = !rt.isAccessExpired();
                    if (!valid) {
                        log.info("Access token expired in DB for userId={}", userId);
                    }
                    return valid;
                })
                .orElse(false);
    }

    /* --------------------------------------------------------------------- */
    /* Skip public endpoints */
    /* --------------------------------------------------------------------- */
    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    /* --------------------------------------------------------------------- */
    /* Send 401 with JSON error */
    /* --------------------------------------------------------------------- */
    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        String json = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        response.getWriter().write(json);
    }

    /* --------------------------------------------------------------------- */
    /* Extract client IP */
    /* --------------------------------------------------------------------- */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }
}