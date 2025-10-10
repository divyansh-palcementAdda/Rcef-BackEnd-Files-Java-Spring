package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.security.JwtProvider;
import com.renaissance.app.security.UserDetailsImpl;
import com.renaissance.app.service.interfaces.IAuthService;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;
import com.renaissance.app.service.interfaces.IUserService;

import jakarta.mail.AuthenticationFailedException;
import jakarta.validation.Valid;

@Service
public class AuthServiceImpl implements IAuthService {

	private final AuthenticationManager authenticationManager;
	private final IUserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final ModelMapper modelMapper;
	private final IOtpService otpService;
	private final IEmailService emailService;
	private final IUserService userService ;

	public AuthServiceImpl(AuthenticationManager authenticationManager, IUserRepository userRepository,
			DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
			ModelMapper modelMapper, IOtpService otpService, IEmailService emailService,IUserService userService) {
		this.authenticationManager = authenticationManager;
		this.userRepository = userRepository;
		this.departmentRepository = departmentRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtProvider = jwtProvider;
		this.modelMapper = modelMapper;
		this.otpService = otpService;
		this.emailService = emailService;
		this.userService = userService;
	}

	@Override
	public JwtResponse login(@Valid LoginRequest loginRequest) throws AuthenticationFailedException {
		String usernameOrEmail = loginRequest.getEmailOrUsername();

		User user = userRepository.findByEmail(usernameOrEmail).or(() -> userRepository.findByUsername(usernameOrEmail))
				.orElseThrow(() -> new AuthenticationFailedException("User not found"));

		if (!user.isEmailVerified()) {
			throw new AuthenticationFailedException("Email not verified. Please verify via OTP before logging in.");
		}

		try {
			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

			Map<String, Object> claims = new HashMap<>();
			claims.put("userId", userDetails.getId());
			claims.put("role", userDetails.getRole().name());

			String jwt = jwtProvider.generateToken(userDetails.getUsername(), claims);
			System.err.println(jwt);
			return JwtResponse.builder().token(jwt).id(userDetails.getId()).email(userDetails.getEmail())
					.username(userDetails.getUsername()).role(userDetails.getRole()).type("Bearer").build();

		} catch (BadCredentialsException ex) {
			throw new AuthenticationFailedException("Invalid credentials");
		}
	}

	@Override
	@Transactional
	public UserDTO register(@Valid UserRequest userRequest) throws AccessDeniedException, ResourcesNotFoundException {
		validateUserRequest(userRequest);
System.err.println("CHECK------------------->");
		Department department = departmentRepository.findById(userRequest.getDepartmentId()).orElseThrow(
				() -> new IllegalArgumentException("Invalid Department ID: " + userRequest.getDepartmentId()));

		User user = modelMapper.map(userRequest, User.class);
		user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
		user.setDepartment(department);
		user.setStatus(UserStatus.ACTIVE);
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		user.setEmailVerified(false); // must verify via OTP
		user.setVerificationToken(UUID.randomUUID().toString());
		user.setEmailVerified(true);
    	user.setVerificationToken(null);
		User saved = userRepository.save(user);

		// Generate OTP and send email (non-blocking recommended; here we call directly)
		String otp = otpService.generateOtp(saved.getEmail());
		try {
			emailService.sendOtpEmail(saved.getEmail(), otp);
		} catch (Exception e) {
			// consider retrying or marking an "emailFailed" flag; for now log and continue
			// but do NOT set emailVerified = true
			e.printStackTrace();
		}

		return modelMapper.map(saved, UserDTO.class);
	}

	@Override
	public void sendVerificationOtp(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));
		if (!otpService.canResend(email)) {
			throw new IllegalArgumentException("OTP resend limit reached. Try again later.");
		}
		String otp = otpService.generateOtp(email);
		otpService.incrementResend(email);
		try {
			emailService.sendOtpEmail(email, otp);
		} catch (Exception e) {
			throw new RuntimeException("Failed to send OTP", e);
		}
	}

	@Override
	@Transactional
	public boolean verifyOtpAndActivate(String email, String otp) {
		boolean valid = otpService.validateOtp(email, otp);
		if (valid) {
			User user = userRepository.findByEmail(email)
					.orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));
			user.setEmailVerified(true);
			user.setVerificationToken(null);
			user.setUpdatedAt(LocalDateTime.now());
			userRepository.save(user);
			otpService.clearOtp(email);
			return true;
		}
		return false;
	}

	// ------------------ helper ------------------
	private void validateUserRequest(UserRequest userRequest) throws AccessDeniedException, ResourcesNotFoundException {
		if (userRequest == null) {
			throw new IllegalArgumentException("User details must be provided");
		}
		if (userRequest.getEmail() == null || userRequest.getEmail().isBlank()) {
			throw new IllegalArgumentException("Email must be provided");
		}
		if (userRepository.existsByEmail(userRequest.getEmail())) { 
			Optional<User> user = userRepository.findByEmail(userRequest.getEmail());
			if(user.get().getStatus().equals(UserStatus.INACTIVE)) {
				user.get().setStatus(UserStatus.ACTIVE);
				userService.updateUser(user.get().getUserId(), userRequest);
			}else {
				throw new IllegalArgumentException("The provided email is already registered: " + userRequest.getEmail());
			}
		
		}
		if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
			throw new IllegalArgumentException("Password must be provided");
		}
		if (userRequest.getDepartmentId() == null || userRequest.getDepartmentId() <= 0) {
			throw new IllegalArgumentException("A valid Department ID must be provided");
		}
	}
}
