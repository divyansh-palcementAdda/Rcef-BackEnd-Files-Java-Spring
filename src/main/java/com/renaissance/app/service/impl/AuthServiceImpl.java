package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.data.redis.core.RedisTemplate;
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
import com.renaissance.app.mapper.UserMapper;
import com.renaissance.app.model.Department;
import com.renaissance.app.model.RefreshToken;
import com.renaissance.app.model.Role;
import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.payload.JwtResponse;
import com.renaissance.app.payload.LoginRequest;
import com.renaissance.app.payload.UserDTO;
import com.renaissance.app.payload.UserRequest;
import com.renaissance.app.repository.DepartmentRepository;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.security.JwtProvider;
import com.renaissance.app.security.JwtUtils;
import com.renaissance.app.security.UserDetailsImpl;
import com.renaissance.app.service.interfaces.IAuthService;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;
import com.renaissance.app.service.interfaces.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
	private static final ExecutorService EMAIL_EXECUTOR = Executors.newFixedThreadPool(2);

	private final AuthenticationManager authenticationManager;
	private final IUserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtProvider jwtProvider;
	private final IOtpService otpService;
	private final IEmailService emailService;
	private final IUserService userService;
	private final UserMapper userMapper;
	private final RefreshTokenService refreshTokenService;   // <-- correct type

	//    private final RedisTemplate<String, String> redisTemplate;
	//    private static final long BLACKLIST_TTL_SECONDS = 7 * 24 * 60 * 60; // 7 days
	//
	//    /* --------------------------------------------------------------------- */
	//    /*                         LOGOUT / REVOKE TOKEN                         */
	//    /* --------------------------------------------------------------------- */
	//    @Transactional
	//    public void revokeRefreshToken(String refreshToken) {
	//        // 1. Blacklist in Redis (fast lookup)
	//        String jti = JwtUtils.extractJti(refreshToken);
	//        if (jti != null) {
	//            redisTemplate.opsForValue()
	//                    .set("blacklist::" + jti, "true", BLACKLIST_TTL_SECONDS, TimeUnit.SECONDS);
	//        }
	//
	//        // 2. Delete from DB (if you store refresh tokens)
	//        refreshTokenService.findByToken(refreshToken)
	//                .ifPresent(refreshTokenService::deleteByUserId());
	//    }

	/* --------------------------------------------------------------------- */
	/*                                 LOGIN                                 */
	/* --------------------------------------------------------------------- */
	@Override
	public JwtResponse login(@Valid LoginRequest loginRequest)
			throws jakarta.mail.AuthenticationFailedException, AccessDeniedException {

		String usernameOrEmail = loginRequest.getEmailOrUsername();

		User user = userRepository.findByEmail(usernameOrEmail)
				.or(() -> userRepository.findByUsername(usernameOrEmail))
				.orElseThrow(() -> new jakarta.mail.AuthenticationFailedException(
						"Invalid credentials or user not found."));

		if (Boolean.FALSE.equals(user.isEmailVerified())) {
			throw new jakarta.mail.AuthenticationFailedException(
					"Email not verified. Please verify before logging in.");
		}

		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new AccessDeniedException("Your account is inactive. Please contact the administrator.");
		}

		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword()));

			SecurityContextHolder.getContext().setAuthentication(authentication);
			UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

			Map<String, Object> claims = new HashMap<>();
			claims.put("userId", userDetails.getId());
			claims.put("role", userDetails.getRole().name());

			String accessToken = jwtProvider.generateToken(userDetails.getUsername(), claims);
			RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

			log.info("User '{}' logged in successfully", usernameOrEmail);

			return JwtResponse.builder()
					.accessToken(accessToken)
					.type("Bearer")
					.id(userDetails.getId())
					.email(userDetails.getEmail())
					.username(userDetails.getUsername())
					.role(userDetails.getRole())
					.refreshToken(refreshToken.getToken())
					.build();

		} catch (BadCredentialsException e) {
			log.warn("Invalid password attempt for {}", usernameOrEmail);
			throw new jakarta.mail.AuthenticationFailedException("Invalid credentials.");
		}
	}

	/* --------------------------------------------------------------------- */
	/*                         REFRESH ACCESS TOKEN                          */
	/* --------------------------------------------------------------------- */
	@Override
	public JwtResponse refreshAccessToken(String refreshToken) throws AccessDeniedException {
		RefreshToken token = refreshTokenService.findByToken(refreshToken)
				.orElseThrow(() -> new AccessDeniedException("Invalid refresh token"));

		RefreshToken validToken = refreshTokenService.verifyExpiration(token);
		User user = validToken.getUser();

		Map<String, Object> claims = Map.of(
				"userId", user.getUserId(),
				"role", user.getRole().name()
				);

		String newAccessToken = jwtProvider.generateToken(user.getEmail(), claims);

		log.info("Access token refreshed for user: {}", user.getEmail());

		return JwtResponse.builder()
				.accessToken(newAccessToken)
				.type("Bearer")
				.id(user.getUserId())
				.email(user.getEmail())
				.username(user.getUsername())
				.role(user.getRole())
				.refreshToken(refreshToken)          // keep the same refresh token
				.build();
	}

	/* --------------------------------------------------------------------- */
	/*                               REGISTER                                */
	/* --------------------------------------------------------------------- */
	@Override
	@Transactional
	public UserDTO register(@Valid UserRequest request)
			throws AccessDeniedException, ResourcesNotFoundException {

		validateUserRequest(request);

		List<Department> departments = departmentRepository.findAllById(request.getDepartmentIds());
		resolveDeprtments(departments,request);
		if (departments.isEmpty()) {
			throw new ResourcesNotFoundException("No valid departments found for given IDs.");
		}

		Optional<User> existingOpt = userRepository.findByEmail(request.getEmail());

		if (existingOpt.isPresent()) {
			User existing = existingOpt.get();

			if (existing.getStatus() == UserStatus.INACTIVE) {
				log.info("Reactivating inactive user: {}", existing.getEmail());
				existing.setStatus(UserStatus.ACTIVE);
				existing.setUpdatedAt(LocalDateTime.now());

				if (request.getPassword() != null && !request.getPassword().isBlank()) {
					existing.setPassword(passwordEncoder.encode(request.getPassword()));
				}

				existing.setDepartments(departments);
				userRepository.save(existing);

				if (Boolean.FALSE.equals(existing.isEmailVerified())) {
					triggerOtpSendAsync(existing.getEmail());
				}

				return userMapper.toDto(existing);
			} else {
				throw new IllegalArgumentException("Email already registered and active: " + request.getEmail());
			}
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setFullName(request.getFullName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setDepartments(departments);
		user.setStatus(UserStatus.ACTIVE);
		user.setRole(request.getRole());
		user.setCreatedAt(LocalDateTime.now());
		user.setUpdatedAt(LocalDateTime.now());
		user.setEmailVerified(true);
		user.setVerificationToken(UUID.randomUUID().toString());

		User saved = userRepository.save(user);
		log.info("New user registered (id={}, email={})", saved.getUserId(), saved.getEmail());
		return userMapper.toDto(saved);
	}

	private void resolveDeprtments(List<Department> departments, @Valid UserRequest request) {
	    if (request.getRole() == null) {
	        return; // No role provided, nothing to validate
	    }

	    Role role = request.getRole();

	    // 🧩 Rule 1: ADMIN / SUB_ADMIN can only be mapped to "Administration"
	    if (role == Role.ADMIN || role == Role.SUB_ADMIN) {
	        for (Department dept : departments) {
	            if (!"Administration".equalsIgnoreCase(dept.getName())) {
	                throw new IllegalArgumentException(
	                    "ADMIN or SUB_ADMIN can only be mapped to 'Administration' department.");
	            }
	        }
	    }

	    // 🧩 Rule 2: HOD can only have one department
	    if (role == Role.HOD) {
	        if (departments == null || departments.isEmpty()) {
	            throw new IllegalArgumentException("HOD must be assigned to exactly one department.");
	        }

	        if (departments.size() > 1) {
	            throw new IllegalArgumentException("HOD cannot be assigned to multiple departments.");
	        }

	        Department department = departments.get(0);

	        // check if department already has an HOD (optional if you have repo)
	        boolean alreadyHasHod = userRepository.existsByRoleAndDepartmentsContaining(Role.HOD, department);
	        if (alreadyHasHod) {
	            throw new IllegalArgumentException(
	                "Department '" + department.getName() + "' already has an assigned HOD.");
	        }
	    }
	}


	/* --------------------------------------------------------------------- */
	/*                         SEND VERIFICATION OTP                         */
	/* --------------------------------------------------------------------- */
	@Override
	public void sendVerificationOtp(String email) throws AccessDeniedException {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new AccessDeniedException("User is inactive. Contact administrator.");
		}

		if (!otpService.canResend(email)) {
			throw new IllegalArgumentException("OTP resend limit reached. Try again later.");
		}

		otpService.incrementResend(email);
		triggerOtpSendAsync(email);

		log.info("OTP resend triggered for {}", email);
	}

	/* --------------------------------------------------------------------- */
	/*                         VERIFY OTP & ACTIVATE                         */
	/* --------------------------------------------------------------------- */
	@Override
	@Transactional
	public boolean verifyOtpAndActivate(String email, String otp) throws AccessDeniedException {
		if (!otpService.validateOtp(email, otp)) {
			log.warn("Invalid OTP for {}", email);
			return false;
		}

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

		if (user.getStatus() == UserStatus.INACTIVE) {
			throw new AccessDeniedException("User account is inactive. Contact administrator.");
		}

		user.setEmailVerified(true);
		user.setVerificationToken(null);
		user.setUpdatedAt(LocalDateTime.now());
		userRepository.save(user);
		otpService.clearOtp(email);

		log.info("User {} successfully verified", email);
		return true;
	}

	/* --------------------------------------------------------------------- */
	/*                               HELPERS                                 */
	/* --------------------------------------------------------------------- */
	private void validateUserRequest(UserRequest req) {
		if (req == null) throw new IllegalArgumentException("User data is required.");
		if (req.getEmail() == null || req.getEmail().isBlank())
			throw new IllegalArgumentException("Email is required.");
		if (req.getUsername() == null || req.getUsername().isBlank())
			throw new IllegalArgumentException("Username is required.");
		if (req.getPassword() == null || req.getPassword().isBlank())
			throw new IllegalArgumentException("Password is required.");
		if (req.getDepartmentIds() == null || req.getDepartmentIds().isEmpty())
			throw new IllegalArgumentException("At least one valid departmentId must be provided.");
	}

	private void triggerOtpSendAsync(String email) {
		EMAIL_EXECUTOR.submit(() -> {
			try {
				String otp = otpService.generateOtp(email);
				emailService.sendOtpEmail(email, otp);
				log.info("OTP email sent successfully to {}", email);
			} catch (Exception e) {
				log.error("Failed to send OTP to {}: {}", email, e.getMessage(), e);
			}
		});
	}
}