package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.service.interfaces.IAuthService;

import jakarta.mail.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final IAuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (AuthenticationFailedException | AccessDeniedException ex) {
            log.warn("Login failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected login error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Login failed due to server error"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRequest userRequest) {
        try {
            UserDTO created = authService.register(userRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException ex) {
            log.warn("Registration failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected registration error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Registration failed"));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            authService.sendVerificationOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (Exception ex) {
            log.warn("Send OTP failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        try {
            boolean ok = authService.verifyOtpAndActivate(email, otp);
            if (ok) {
                return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
            }
        } catch (AccessDeniedException ex) {
            log.warn("OTP verification failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected OTP verification error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "OTP verification failed"));
        }
    }
}
