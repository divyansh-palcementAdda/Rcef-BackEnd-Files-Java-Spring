package com.renaissance.app.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;
import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/api/otp")
@CrossOrigin(origins = "http://localhost:4200") 
public class OtpController {

	@Autowired
	private IOtpService otpService;

	@Autowired
	private IEmailService emailService;

	@Autowired
	
	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		if (email == null || email.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
		}
		String otp = otpService.generateOtp(email);
		System.err.println(otp);
		try {
			emailService.sendOtpEmail(email, otp);
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
		String email = request.get("email");
		String otp = request.get("otp");

		if (email == null || otp == null || email.isBlank() || otp.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("message", "Email and OTP are required"));
		}
		
		boolean valid = otpService.validateOtp(email, otp);
		if (valid) {
			otpService.clearOtp(email);
//			userService.validateUser(email);
			return ResponseEntity.ok(Map.of("success", true, "message", "OTP verified"));
		} else {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid or expired OTP"));
		}
	}
}
