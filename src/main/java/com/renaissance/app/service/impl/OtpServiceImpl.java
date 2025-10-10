package com.renaissance.app.service.impl;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.renaissance.app.model.UserStatus;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.service.interfaces.IOtpService;

@Service
public class OtpServiceImpl implements IOtpService {

    private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> resendCount = new ConcurrentHashMap<>();

    private static final long OTP_VALID_DURATION = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_RESEND_PER_HOUR = 5;
    private static final SecureRandom secureRandom = new SecureRandom();
    @Autowired
    private IUserRepository userRepository ;

    @Override
    public String generateOtp(String email) {
    	if(userRepository.findByEmail(email).get().getStatus().equals(UserStatus.ACTIVE)){
			throw new IllegalArgumentException("The provided email is already registered: " +email);
    	}
        int code = secureRandom.nextInt(1_000_000);
        String otp = String.format("%06d", code);
        otpStorage.put(email, otp);
        expiryMap.put(email, System.currentTimeMillis() + OTP_VALID_DURATION);
        resendCount.putIfAbsent(email, 0);
        return otp;
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        String current = otpStorage.get(email);
        Long expiry = expiryMap.get(email);
        if (current == null || expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            clearOtp(email);
            return false;
        }
        return current.equals(otp);
    }

    @Override
    public void clearOtp(String email) {
        otpStorage.remove(email);
        expiryMap.remove(email);
        resendCount.remove(email);
    }

    @Override
    public boolean canResend(String email) {
        return resendCount.getOrDefault(email, 0) < MAX_RESEND_PER_HOUR;
    }

    @Override
    public void incrementResend(String email) {
        resendCount.merge(email, 1, Integer::sum);
    }
}
