package com.renaissance.app.service.interfaces;

public interface IOtpService {
	String generateOtp(String email);

	boolean validateOtp(String email, String otp);

	void clearOtp(String email);

	boolean canResend(String email);

	void incrementResend(String email);
}
