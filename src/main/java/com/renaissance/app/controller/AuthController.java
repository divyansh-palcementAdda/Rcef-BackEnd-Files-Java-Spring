package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.service.interfaces.IAuthService;

import jakarta.mail.AuthenticationFailedException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:4200"}, allowCredentials = "true")
public class AuthController {

    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            JwtResponse jwtResponse = authService.login(loginRequest);
            return ResponseEntity.ok(jwtResponse);
        } catch (AuthenticationFailedException ex) {
        	System.err.println(ex.getMessage());
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED)
                    .body(Map.of("message", ex.getMessage()));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        try {
            UserDTO created = authService.register(userRequest);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException ex) {
        	System.err.println(ex.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
//            ex.printStackTrace();
        	System.err.println(ex.getMessage());
            return ResponseEntity.status(500).body(Map.of("message", "Registration failed"));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestParam String email) {
        try {
            authService.sendVerificationOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        boolean ok = authService.verifyOtpAndActivate(email, otp);
        if (ok) {
            return ResponseEntity.ok(Map.of("message", "Email verified"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP"));
        }
    }
}
