package com.renaissance.app.service.interfaces;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.AuthenticationFailedException;
import com.renaissance.app.exception.BadRequestException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;

import jakarta.servlet.http.HttpServletRequest;

public interface IAuthService {
	    JwtResponse login(LoginRequest loginRequest, HttpServletRequest request)
	            throws AuthenticationFailedException, AccessDeniedException, ResourcesNotFoundException;

	    JwtResponse refreshAccessToken(String refreshToken) throws AccessDeniedException, BadRequestException;

	    void logout(String refreshToken);

	    void logoutAll(Long userId);

	    UserDTO register(UserRequest request) throws AccessDeniedException, ResourcesNotFoundException;

	    void sendVerificationOtp(String email) throws AccessDeniedException;

	    boolean verifyOtpAndActivate(String email, String otp) throws AccessDeniedException;
}
