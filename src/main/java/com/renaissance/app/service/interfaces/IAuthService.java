package com.renaissance.app.service.interfaces;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import jakarta.mail.AuthenticationFailedException;

public interface IAuthService {
	JwtResponse login(LoginRequest loginRequest) throws AuthenticationFailedException;

	UserDTO register(UserRequest userRequest) throws AccessDeniedException, ResourcesNotFoundException;

	void sendVerificationOtp(String email);

	boolean verifyOtpAndActivate(String email, String otp);
}
