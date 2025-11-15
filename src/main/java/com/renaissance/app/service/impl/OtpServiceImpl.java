package com.renaissance.app.service.impl;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.renaissance.app.model.User;
import com.renaissance.app.model.UserStatus;
import com.renaissance.app.repository.IUserRepository;
import com.renaissance.app.service.interfaces.IOtpService;

@Service
public class OtpServiceImpl implements IOtpService {

	private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

	private final Map<String, String> otpStorage = new ConcurrentHashMap<>();
	private final Map<String, Long> expiryMap = new ConcurrentHashMap<>();

	// Resend control: tracks attempts and the window start (ms)
	private final Map<String, Integer> resendCount = new ConcurrentHashMap<>();
	private final Map<String, Long> resendWindowStart = new ConcurrentHashMap<>();

	private static final long OTP_VALID_DURATION_MS = TimeUnit.MINUTES.toMillis(5);
	private static final int MAX_RESEND_PER_HOUR = 5;
	private static final long RESEND_WINDOW_MS = TimeUnit.HOURS.toMillis(1);

	private static final SecureRandom secureRandom = new SecureRandom();

	private final IUserRepository userRepository;

	public OtpServiceImpl(IUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public String generateOtp(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required for OTP generation.");
		}

		Optional<User> existingUserOpt = userRepository.findByEmail(email);
		if (existingUserOpt.isPresent()) {
			User user = existingUserOpt.get();

			if (user.getStatus() == UserStatus.ACTIVE) {
				if (!user.isEmailVerified()) {
				throw new IllegalArgumentException("This email is already registered but not yet verified.");
			}
				throw new IllegalArgumentException("This email is already registered with an active user.");
			}

			if (user.getStatus() == UserStatus.INACTIVE) {
				throw new IllegalArgumentException(
						"This email is already registered but marked as inactive on user id "+user.getUserId()+". Please reactivate the user.");
			}

			
		}

		// Generate a 6-digit OTP
		int code = secureRandom.nextInt(1_000_000);
		String otp = String.format("%06d", code);

		// Store OTP with expiration time
		otpStorage.put(email, otp);
		expiryMap.put(email, System.currentTimeMillis() + OTP_VALID_DURATION_MS);

		// Initialize resend tracking if not already present
		resendCount.putIfAbsent(email, 0);
		resendWindowStart.putIfAbsent(email, System.currentTimeMillis());

		log.debug("Generated OTP for {} (expires in {} ms)", email, OTP_VALID_DURATION_MS);
		return otp;
	}

	@Override
	public boolean validateOtp(String email, String otp) {
		if (email == null || otp == null)
			return false;
		String current = otpStorage.get(email);
		Long expiry = expiryMap.get(email);

		if (current == null || expiry == null)
			return false;

		if (System.currentTimeMillis() > expiry) {
			clearOtp(email);
			log.debug("OTP expired for {}", email);
			return false;
		}

		boolean valid = current.equals(otp);
		if (valid) {
			// On successful validation we'll clear OTP (caller may also clear)
			clearOtp(email);
			log.debug("OTP validated for {}", email);

		} else {
			log.debug("Invalid OTP attempt for {}", email);
		}
		return valid;
	}

	@Override
	public void clearOtp(String email) {
		otpStorage.remove(email);
		expiryMap.remove(email);
		resendCount.remove(email);
		resendWindowStart.remove(email);
		log.debug("Cleared OTP data for {}", email);
	}

	@Override
	public boolean canResend(String email) {
		if (email == null)
			return false;
		long now = System.currentTimeMillis();
		resendWindowStart.putIfAbsent(email, now);
		synchronized (getSyncObject(email)) {
			long windowStart = resendWindowStart.getOrDefault(email, now);
			if (now - windowStart > RESEND_WINDOW_MS) {
				// reset window
				resendWindowStart.put(email, now);
				resendCount.put(email, 0);
				return true;
			}
			return resendCount.getOrDefault(email, 0) < MAX_RESEND_PER_HOUR;
		}
	}

	@Override
	public void incrementResend(String email) {
		if (email == null)
			return;
		long now = System.currentTimeMillis();
		resendWindowStart.putIfAbsent(email, now);
		synchronized (getSyncObject(email)) {
			long windowStart = resendWindowStart.getOrDefault(email, now);
			if (now - windowStart > RESEND_WINDOW_MS) {
				// reset the window
				resendWindowStart.put(email, now);
				resendCount.put(email, 1);
			} else {
				resendCount.merge(email, 1, Integer::sum);
			}
			log.debug("Resend count for {} = {}", email, resendCount.get(email));
		}
	}

	@Override
	public boolean hasOtp(String email) {
		return otpStorage.containsKey(email) && expiryMap.containsKey(email)
				&& System.currentTimeMillis() < expiryMap.get(email);
	}

	@Override
	public String getOtpForEmail(String email) {
		if (!hasOtp(email))
			return null;
		return otpStorage.get(email);
	}

	// small per-email sync object (forces unique String key; ok for moderate scale)
	private Object getSyncObject(String email) {
		return email.intern();
	}
}