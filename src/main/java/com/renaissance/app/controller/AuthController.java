package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.AuthenticationFailedException;
import com.renaissance.app.payload.ApiResult;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.service.interfaces.IAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST API for Authentication
 * - Uses ApiResult<T> for standardized responses
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
public class AuthController {

    private final IAuthService authService;

    /* --------------------------------------------------------------------- */
    /* LOGIN */
    /* --------------------------------------------------------------------- */
    @PostMapping("/login")
    public ResponseEntity<ApiResult<JwtResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest) {

        try {
            JwtResponse jwtResponse = authService.login(loginRequest, httpRequest);
            log.info("LOGIN SUCCESS | User: {}", loginRequest.getEmailOrUsername());

            return ResponseEntity.ok(
                    ApiResult.ok(jwtResponse, "Login successful")
            );

        } catch (AuthenticationFailedException e) {
            log.warn("LOGIN FAILED | Invalid credentials: {}", loginRequest.getEmailOrUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error("Invalid email/username or password", HttpStatus.UNAUTHORIZED));

        } catch (AccessDeniedException e) {
            log.warn("LOGIN BLOCKED | {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.FORBIDDEN));

        } catch (Exception e) {
            log.error("LOGIN ERROR | Unexpected failure", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Login failed. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* REFRESH TOKEN */
    /* --------------------------------------------------------------------- */
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResult<JwtResponse>> refreshToken(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("refreshToken is required", HttpStatus.BAD_REQUEST));
        }

        try {
            JwtResponse jwtResponse = authService.refreshAccessToken(refreshToken);
            log.info("TOKEN REFRESHED | Success");

            return ResponseEntity.ok(
                    ApiResult.ok(jwtResponse, "Token refreshed successfully")
            );

        } catch (AccessDeniedException e) {
            log.warn("TOKEN REFRESH FAILED | Invalid/expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResult.error("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED));

        } catch (Exception e) {
            log.error("TOKEN REFRESH ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Failed to refresh token", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* LOGOUT (Per Device) */
    /* --------------------------------------------------------------------- */
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(@RequestBody Map<String, String> payload) {
        String refreshToken = payload.get("refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("refreshToken is required", HttpStatus.BAD_REQUEST));
        }

        try {
            authService.logout(refreshToken);
            log.info("LOGOUT SUCCESS | Session terminated");
            return ResponseEntity.ok(
                    ApiResult.ok(null, "Logged out successfully")
            );

        } catch (Exception e) {
            log.error("LOGOUT ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Logout failed", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* GLOBAL LOGOUT (All Devices) */
    /* --------------------------------------------------------------------- */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResult<Void>> logoutAll(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");

        if (userId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("userId is required", HttpStatus.BAD_REQUEST));
        }

        try {
            authService.logoutAll(userId);
            log.info("GLOBAL LOGOUT | All sessions revoked for userId={}", userId);
            return ResponseEntity.ok(
                    ApiResult.ok(null, "All sessions logged out successfully")
            );

        } catch (Exception e) {
            log.error("GLOBAL LOGOUT ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Global logout failed", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* REGISTER */
    /* --------------------------------------------------------------------- */
    @PostMapping("/register")
    public ResponseEntity<ApiResult<UserDTO>> register(@Valid @RequestBody UserRequest userRequest) {
        try {
            UserDTO user = authService.register(userRequest);
            log.info("REGISTER SUCCESS | User: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResult.ok(user, "User registered. OTP sent to email."));

        } catch (IllegalArgumentException e) {
            log.warn("REGISTER FAILED | Validation: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));

        } catch (Exception e) {
            log.error("REGISTER ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Registration failed", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* SEND OTP */
    /* --------------------------------------------------------------------- */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResult<Void>> sendOtp(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("Email is required", HttpStatus.BAD_REQUEST));
        }

        try {
            authService.sendVerificationOtp(email);
            log.info("OTP SENT | Email: {}", email);
            return ResponseEntity.ok(
                    ApiResult.ok(null, "OTP sent successfully to " + email)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error(e.getMessage(), HttpStatus.BAD_REQUEST));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.FORBIDDEN));

        } catch (Exception e) {
            log.error("OTP SEND ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("Failed to send OTP", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    /* --------------------------------------------------------------------- */
    /* VERIFY OTP */
    /* --------------------------------------------------------------------- */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResult<Void>> verifyOtp(
            @RequestParam String email,
            @RequestParam String otp) {

        if (email == null || email.isBlank() || otp == null || otp.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResult.error("Email and OTP are required", HttpStatus.BAD_REQUEST));
        }

        try {
            boolean verified = authService.verifyOtpAndActivate(email, otp);
            if (verified) {
                log.info("OTP VERIFIED | Email: {}", email);
                return ResponseEntity.ok(
                        ApiResult.ok(null, "Email verified successfully")
                );
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResult.error("Invalid or expired OTP", HttpStatus.BAD_REQUEST));
            }

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResult.error(e.getMessage(), HttpStatus.FORBIDDEN));

        } catch (Exception e) {
            log.error("OTP VERIFY ERROR", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResult.error("OTP verification failed", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }
}