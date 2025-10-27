package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.ResourcesNotFoundException;
import com.renaissance.app.mapper.UserMapper;
import com.renaissance.app.model.*;
import com.renaissance.app.payload.*;
import com.renaissance.app.repository.*;
import com.renaissance.app.security.JwtProvider;
import com.renaissance.app.security.UserDetailsImpl;
import com.renaissance.app.service.interfaces.*;

import jakarta.mail.AuthenticationFailedException;
import jakarta.validation.Valid;

@Service
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

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            IUserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            IOtpService otpService,
            IEmailService emailService,
            IUserService userService,
            UserMapper userMapper
    ) {
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
    public JwtResponse login(@Valid LoginRequest loginRequest)
            throws AuthenticationFailedException, AccessDeniedException {

        String usernameOrEmail = loginRequest.getEmailOrUsername();

        User user = userRepository.findByEmail(usernameOrEmail)
                .or(() -> userRepository.findByUsername(usernameOrEmail))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials or user not found."));

        if (!Boolean.TRUE.equals(user.isEmailVerified())) {
            throw new AuthenticationFailedException("Email not verified. Please verify before logging in.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException("Your account is inactive. Please contact the administrator.");
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

            String token = jwtProvider.generateToken(userDetails.getUsername(), claims);

            log.info("✅ User '{}' logged in successfully", usernameOrEmail);

            return JwtResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .id(userDetails.getId())
                    .email(userDetails.getEmail())
                    .username(userDetails.getUsername())
                    .role(userDetails.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("❌ Invalid password attempt for {}", usernameOrEmail);
            throw new AuthenticationFailedException("Invalid credentials.");
        }
    }

    // ---------------------- REGISTER ----------------------
    @Override
    @Transactional
    public UserDTO register(@Valid UserRequest request)
            throws AccessDeniedException, ResourcesNotFoundException {

        validateUserRequest(request);

        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourcesNotFoundException("Department not found for ID: " + request.getDepartmentId()));

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

                existing.setDepartment(department);
                userRepository.save(existing);

                if (!Boolean.TRUE.equals(existing.isEmailVerified())) {
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
        user.setDepartment(department);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(request.getRole());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setVerificationToken(UUID.randomUUID().toString());

        User saved = userRepository.save(user);

//        triggerOtpSendAsync(saved.getEmail());

        log.info("🟢 New user registered (id={}, email={})", saved.getUserId(), saved.getEmail());
        return userMapper.toDto(saved);
    }

    // ---------------------- SEND OTP ----------------------
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

        log.info("🔁 OTP resend triggered for {}", email);
    }

    // ---------------------- VERIFY OTP ----------------------
    @Override
    @Transactional
    public boolean verifyOtpAndActivate(String email, String otp) throws AccessDeniedException {
        if (!otpService.validateOtp(email, otp)) {
            log.warn("❌ Invalid OTP for {}", email);
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

        log.info("✅ User {} successfully verified", email);
        return true;
    }

    // ---------------------- HELPERS ----------------------
    private void validateUserRequest(UserRequest req) {
        if (req == null) throw new IllegalArgumentException("User data is required.");
        if (req.getEmail() == null || req.getEmail().isBlank())
            throw new IllegalArgumentException("Email is required.");
        if (req.getUsername() == null || req.getUsername().isBlank())
            throw new IllegalArgumentException("Username is required.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("Password is required.");
        if (req.getDepartmentId() == null || req.getDepartmentId() <= 0)
            throw new IllegalArgumentException("A valid departmentId must be provided.");
    }

    private void triggerOtpSendAsync(String email) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                String otp = otpService.generateOtp(email);
                emailService.sendOtpEmail(email, otp);
                log.info("📩 OTP email sent successfully to {}", email);
            } catch (Exception e) {
                log.error("Failed to send OTP to {}: {}", email, e.getMessage(), e);
            }
        });
    }
}
