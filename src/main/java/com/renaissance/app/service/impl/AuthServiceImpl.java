package com.renaissance.app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.renaissance.app.exception.AccessDeniedException;
import com.renaissance.app.exception.AuthenticationFailedException;
import com.renaissance.app.exception.BadRequestException;
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
import com.renaissance.app.security.UserDetailsImpl;
import com.renaissance.app.service.interfaces.IAuthService;
import com.renaissance.app.service.interfaces.IEmailService;
import com.renaissance.app.service.interfaces.IOtpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final ExecutorService EMAIL_EXECUTOR = Executors.newFixedThreadPool(2);

    private final AuthenticationManager authenticationManager;
    private final IUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final IOtpService otpService;
    private final IEmailService emailService;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    /* --------------------------------------------------------------------- */
    /* LOGIN – Multi-Device Support */
    /* --------------------------------------------------------------------- */
    @Override
    @Transactional
    public JwtResponse login(@Valid LoginRequest loginRequest, HttpServletRequest httpRequest)
            throws AuthenticationFailedException, AccessDeniedException, ResourcesNotFoundException {

        String identifier = loginRequest.getEmailOrUsername();

        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new AuthenticationFailedException("Invalid credentials or user not found."));

        if (Boolean.FALSE.equals(user.isEmailVerified())) {
            throw new AuthenticationFailedException("Email not verified. Please verify your email first.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException("Your account is inactive. Please contact the administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            String clientIp = extractClientIp(httpRequest);
            String deviceInfo = extractDeviceInfo(httpRequest);

            Map<String, Object> claims = Map.of(
                    "userId", userDetails.getId(),
                    "role", userDetails.getRole().name()
            );

         // In AuthServiceImpl
            String accessToken = jwtProvider.generateAccessToken(
                user.getEmail(),
                Map.of("userId", user.getUserId(), "role", user.getRole().name())
            );

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    user, accessToken, clientIp, deviceInfo
            );

            log.info("LOGIN SUCCESS | User: {} | IP: {} | Device: {}", identifier, clientIp, deviceInfo);

            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getRefreshToken())
                    .type("Bearer")
                    .id(userDetails.getId())
                    .email(userDetails.getEmail())
                    .username(userDetails.getUsername())
                    .role(userDetails.getRole())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("LOGIN FAILED | Invalid password for: {}", identifier);
            throw new AuthenticationFailedException("Invalid credentials.");
        }
    }

    /* --------------------------------------------------------------------- */
    /* REFRESH ACCESS TOKEN – Secure Rotation */
    /* --------------------------------------------------------------------- */
    @Override
    @Transactional
    public JwtResponse refreshAccessToken(String refreshToken) throws AccessDeniedException, BadRequestException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token is required");
        }

        RefreshToken rt = refreshTokenService.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AccessDeniedException("Invalid or expired refresh token"));

        RefreshToken updated = refreshTokenService.verifyAndRefresh(rt);

        User user = updated.getUser();

        log.info("TOKEN REFRESHED | User: {} | IP: {}", user.getEmail(), updated.getClientIp());

        return JwtResponse.builder()
                .accessToken(updated.getAccessToken())
                .refreshToken(updated.getRefreshToken())
                .type("Bearer")
                .id(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    /* --------------------------------------------------------------------- */
    /* LOGOUT – Per Device */
    /* --------------------------------------------------------------------- */
    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeByRefreshToken(refreshToken);
            log.info("LOGOUT | Refresh token revoked");
        }
    }

    /* --------------------------------------------------------------------- */
    /* GLOBAL LOGOUT – All Devices */
    /* --------------------------------------------------------------------- */
    @Override
    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllByUserId(userId);
        log.info("GLOBAL LOGOUT | All sessions revoked for userId={}", userId);
    }

    /* --------------------------------------------------------------------- */
    /* REGISTER USER */
    /* --------------------------------------------------------------------- */
    @Override
    @Transactional
    public UserDTO register(@Valid UserRequest request)
            throws AccessDeniedException, ResourcesNotFoundException {

        validateUserRequest(request);

        List<Department> departments = departmentRepository.findAllById(request.getDepartmentIds());
        if (departments.isEmpty()) {
            throw new ResourcesNotFoundException("No valid departments found for given IDs.");
        }

        resolveDepartmentRules(departments, request);

        Optional<User> existingOpt = userRepository.findByEmail(request.getEmail());
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (existing.getStatus() == UserStatus.INACTIVE) {
                log.info("Reactivating inactive user: {}", existing.getEmail());
                existing.setStatus(UserStatus.ACTIVE);
                existing.setUpdatedAt(LocalDateTime.now());
                if (!request.getPassword().isBlank()) {
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

        User user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .departments(departments)
                .status(UserStatus.ACTIVE)
                .role(request.getRole())
                .emailVerified(true)
                .verificationToken(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);
//        triggerOtpSendAsync(saved.getEmail());
        log.info("New user registered | ID: {} | Email: {}", saved.getUserId(), saved.getEmail());
        return userMapper.toDto(saved);
    }

    /* --------------------------------------------------------------------- */
    /* SEND OTP */
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
    /* VERIFY OTP & ACTIVATE EMAIL */
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

        log.info("Email verified successfully for {}", email);
        return true;
    }

    /* --------------------------------------------------------------------- */
    /* UTILITIES */
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
            throw new IllegalArgumentException("At least one department ID is required.");
    }

    private void resolveDepartmentRules(List<Department> departments, UserRequest request) {
        Role role = request.getRole();
        if (role == null) return;

        if (role == Role.ADMIN || role == Role.SUB_ADMIN) {
            boolean valid = departments.stream()
                    .allMatch(d -> "Administration".equalsIgnoreCase(d.getName()));
            if (!valid) {
                throw new IllegalArgumentException("ADMIN/SUB_ADMIN can only be in 'Administration' department.");
            }
        }

        if (role == Role.HOD) {
            if (departments.size() != 1) {
                throw new IllegalArgumentException("HOD must be assigned to exactly one department.");
            }
            Department dept = departments.get(0);
            boolean hasHod = userRepository.existsByRoleAndDepartmentsContaining(Role.HOD, dept);
            if (hasHod) {
                throw new IllegalArgumentException("Department '" + dept.getName() + "' already has an HOD.");
            }
        }
    }

    private void triggerOtpSendAsync(String email) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                String otp = otpService.generateOtp(email);
                emailService.sendOtpEmail(email, otp);
                log.info("OTP email sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send OTP to {}: {}", email, e.getMessage(), e);
            }
        });
    }

    public static String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }

    public static String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return (userAgent != null && userAgent.length() > 512) ? userAgent.substring(0, 512) : userAgent;
    }

    // Helper for testing without HTTP context
    public static String getCurrentClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? extractClientIp(attrs.getRequest()) : "unknown";
    }

	
}