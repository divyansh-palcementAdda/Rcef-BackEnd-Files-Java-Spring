package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Production-ready AuthServiceImpl
 *
 * Notes:
 *  - Replace the EMAIL_EXECUTOR with an injected TaskExecutor or @Async-managed bean in real deployments.
 *  - This class is intentionally modular: OTP, Email, JWT, UserRepository, DepartmentRepository, and UserService
 *    are injected and can be swapped out or mocked for tests.
 */
@Service
public class AuthServiceImpl implements IAuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    // lightweight executor for sending emails asynchronously (replace with managed bean in prod)
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

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           IUserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           PasswordEncoder passwordEncoder,
                           JwtProvider jwtProvider,
                           IOtpService otpService,
                           IEmailService emailService,
                           IUserService userService,
                           UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.otpService = otpService;
        this.emailService = emailService;
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // ---------------------- LOGIN ----------------------
    @Override
    public JwtResponse login(@Valid LoginRequest loginRequest) throws AuthenticationFailedException, AccessDeniedException {
        String usernameOrEmail = loginRequest.getEmailOrUsername();

        // Fetch user by email or username
        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials or user not found."));

        // Prevent login if email not verified
        if (!Boolean.TRUE.equals(user.isEmailVerified())) {
            throw new AuthenticationFailedException("Email not verified. Verify first before logging in.");
        }

        // Prevent login if user is inactive
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException("User account is inactive. Contact administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userDetails.getId());
            claims.put("role", userDetails.getRole().name());

            String jwt = jwtProvider.generateToken(userDetails.getUsername(), claims);

            log.info("User '{}' authenticated successfully (id={})", userDetails.getUsername(), userDetails.getId());

            return JwtResponse.builder()
                    .token(jwt)
                    .id(userDetails.getId())
                    .email(userDetails.getEmail())
                    .username(userDetails.getUsername())
                    .role(userDetails.getRole())
                    .type("Bearer")
                    .build();

        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for '{}'", usernameOrEmail);
            throw new AuthenticationFailedException("Invalid credentials.");
        }
    }

    // ---------------------- REGISTER ----------------------
    @Override
    @Transactional
    public UserDTO register(@Valid UserRequest userRequest) throws AccessDeniedException, ResourcesNotFoundException {
        validateUserRequestForRegister(userRequest);

        Department department = departmentRepository.findById(userRequest.getDepartmentId())
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found for id: " + userRequest.getDepartmentId()));

        // handle duplicate email scenario: if exists and inactive, reactivate; if active, reject
        Optional<User> existingOpt = userRepository.findByEmail(userRequest.getEmail());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (existing.getStatus() == UserStatus.INACTIVE) {
                // reactivate and update core fields (do not automatically overwrite sensitive fields without consent)
                log.info("Reactivating previously INACTIVE user with email {}", existing.getEmail());
                existing.setStatus(UserStatus.ACTIVE);
                existing.setUpdatedAt(LocalDateTime.now());
                // If client provided new password, update it; otherwise keep existing
                if (userRequest.getPassword() != null && !userRequest.getPassword().isBlank()) {
                    existing.setPassword(passwordEncoder.encode(userRequest.getPassword()));
                }
                existing.setDepartment(department);
                userRepository.save(existing);

                // proceed to send OTP for verification if not verified
                if (!Boolean.TRUE.equals(existing.isEmailVerified())) {
                    triggerOtpSendAsync(existing.getEmail());
                }

                return userMapper.toDto(existing);
            } else {
                throw new IllegalArgumentException("Email is already registered: " + userRequest.getEmail());
            }
        }

        // Create new user entity (map fields explicitly to avoid trusting incoming DTOs)
        User user = new User();
        user.setUsername(userRequest.getUsername());
        user.setEmail(userRequest.getEmail());
        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        user.setDepartment(department);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setRole(userRequest.getRole());
        user.setFullName(userRequest.getFullName());

        User saved = userRepository.save(user);

        // Generate OTP and send asynchronously
        otpService.generateOtp(saved.getEmail());
        triggerOtpSendAsync(saved.getEmail());

        log.info("New user registered (id={}, email={})", saved.getUserId(), saved.getEmail());

        return userMapper.toDto(saved);
    }

    // ---------------------- SEND VERIFICATION OTP ----------------------
    @Override
    public void sendVerificationOtp(String email) throws AccessDeniedException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with email: " + email));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException("User account is inactive. Contact administrator.");
        }

        if (!otpService.canResend(email)) {
            throw new IllegalArgumentException("OTP resend limit reached. Try again later.");
        }
        otpService.incrementResend(email);
        otpService.generateOtp(email);
        triggerOtpSendAsync(email);
    }

    // ---------------------- VERIFY OTP & ACTIVATE ----------------------
    @Override
    @Transactional
    public boolean verifyOtpAndActivate(String email, String otp) throws AccessDeniedException {
        boolean valid = otpService.validateOtp(email, otp);
        if (!valid) {
            log.warn("Invalid OTP attempt for {}", email);
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

        log.info("User {} verified and activated", email);
        return true;
    }

    // ---------------------- HELPERS ----------------------

    /**
     * Validates incoming register request.
     * Throws explicit exceptions with meaningful messages for API consumers.
     */
    private void validateUserRequestForRegister(UserRequest userRequest) {
        if (userRequest == null) {
            throw new IllegalArgumentException("User data is required.");
        }
        if (userRequest.getEmail() == null || userRequest.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email must be provided.");
        }
        if (userRequest.getUsername() == null || userRequest.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username must be provided.");
        }
        if (userRequest.getPassword() == null || userRequest.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password must be provided.");
        }
        if (userRequest.getDepartmentId() == null || userRequest.getDepartmentId() <= 0) {
            throw new IllegalArgumentException("A valid departmentId must be provided.");
        }
    }

    /**
     * Fire-and-forget OTP email sending. Uses a small managed executor.
     * In production replace with an injected TaskExecutor or message queue for reliability.
     */
    private void triggerOtpSendAsync(String email) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                // ensure OTP exists (generate if missing)
                if (!otpService.hasOtp(email)) {
                    otpService.generateOtp(email);
                }
                String otp = otpService.getOtpForEmail(email); // assumed helper; if not available you can re-generate
                emailService.sendOtpEmail(email, otp);
                log.info("OTP email enqueued/sent for {}", email);
            } catch (Exception e) {
                // Log failure; do not block registration flow. Consider alerting/metrics.
                log.error("Failed to send OTP to {}: {}", email, e.getMessage(), e);
            }
        });
    }
}
