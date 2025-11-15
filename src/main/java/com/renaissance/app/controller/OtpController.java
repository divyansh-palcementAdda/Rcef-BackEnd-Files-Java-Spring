package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.model.User;
import com.renaissance.app.payload.OtpRequest;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;

import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/otp")
//@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
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
            log.info("✅ OTP generated for email: {}", email);
            log.info("✅ OTP generated for email: {}", otp);

            emailService.sendOtpEmail(email, otp);
            log.info("📧 OTP email sent successfully to {}", email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", "OTP_SENT",
                    "message", "OTP sent successfully to your email."
            ));

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            log.warn("⚠️ OTP generation failed for {}: {}", email, errorMessage);

            // Custom handling for inactive user case
            if (errorMessage.contains("inactive")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "status", "INACTIVE_USER",
                                "message", errorMessage,
                                "redirect", true
                        ));
            }

            // Custom handling for already active user
            if (errorMessage.contains("active user")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                                "success", false,
                                "status", "ACTIVE_USER",
                                "message", errorMessage,
                                "redirect", false
                        ));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "success", false,
                            "status", "FAILED",
                            "message", errorMessage
                    ));

        } catch (MessagingException e) {
            log.error("❌ Failed to send OTP email to {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "status", "EMAIL_FAILED",
                            "message", "Failed to send OTP email. Please try again later."
                    ));

        } catch (Exception e) {
            log.error("🚨 Unexpected error while generating OTP for {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "status", "ERROR",
                            "message", "An unexpected error occurred while processing your request."
                    ));
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