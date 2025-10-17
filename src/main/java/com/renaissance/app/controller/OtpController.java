package com.renaissance.app.controller;

import com.renaissance.app.model.User;
import com.renaissance.app.payload.OtpRequest;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final IOtpService otpService;
    private final IEmailService emailService;
    private final IUserRepository userRepository;

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpRequest request) {
        String email = request.getEmail();

        try {
            String otp = otpService.generateOtp(email);
            log.info("Generated OTP for email {}: {}", email, otp); // Logging instead of System.err
            emailService.sendOtpEmail(email, otp);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to send OTP"));
        } catch (Exception e) {
            log.error("Unexpected error while generating OTP for {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not generate OTP"));
        }
    }
    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody OtpRequest request){
    	System.err.println("going to verify");
    	User user = userRepository.findByEmail(request.getEmail()).get();
    	System.err.println(user);
    	user.setEmailVerified(true);
    	System.err.println(user);
    	return ResponseEntity.ok(userRepository.save(user));
    	
    	
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        if (otp == null || otp.isBlank()) {
            log.warn("OTP verification failed for {}: OTP missing", email);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "OTP is required"));
        }

        try {
            boolean valid = otpService.validateOtp(email, otp);
            if (valid) {
                otpService.clearOtp(email);
                log.info("OTP verified successfully for {}", email);
                return ResponseEntity.ok(Map.of("success", true, "message", "OTP verified"));
            } else {
                log.warn("Invalid or expired OTP for {}", email);
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired OTP"));
            }
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification for {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "OTP verification failed"));
        }
    }
}
