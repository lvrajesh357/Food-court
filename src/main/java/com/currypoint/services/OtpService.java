package com.currypoint.services;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Simple in-memory OTP store for email-based login/registration.
 *
 * Not backed by a database table on purpose - OTPs are short-lived (5
 * minutes) and only ever need to survive until the user types them back in,
 * so keeping them in memory avoids extra schema/migration work. They're
 * lost if the app restarts, which is fine (the user just requests a new
 * one).
 *
 * The OTP goes ONLY to the customer's own email - never to the owner.
 * Owner notifications are a completely separate thing that happens later,
 * only after an order is actually placed (see MyController.confirmPayment).
 */
@Service
public class OtpService {

	private static final long OTP_VALID_MILLIS = 5 * 60 * 1000; // 5 minutes

	private static class OtpEntry {
		final String code;
		final long expiresAt;

		OtpEntry(String code, long expiresAt) {
			this.code = code;
			this.expiresAt = expiresAt;
		}
	}

	private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	@Autowired
	private EmailService emailService;

	/**
	 * Generates a new 6-digit OTP for this email address and sends it via
	 * email - only to that address, nobody else.
	 */
	public String generateAndSend(String email) {
		String code = String.format("%06d", random.nextInt(1_000_000));
		otpStore.put(email, new OtpEntry(code, System.currentTimeMillis() + OTP_VALID_MILLIS));

		String subject = "Your FoodCourt login OTP";
		String message = "Your FoodCourt verification code is " + code + ". It expires in 5 minutes.\n\n"
				+ "If you didn't request this, you can safely ignore this email.";
		emailService.sendMail(email, subject, message);

		// Also print to the console so login/registration is testable even
		// before Gmail SMTP is configured with a real app password.
		System.out.println("[OTP] Code for " + email + " is " + code + " (valid 5 minutes)");

		return code;
	}

	public boolean verify(String email, String code) {
		OtpEntry entry = otpStore.get(email);
		if (entry == null) {
			return false;
		}
		if (System.currentTimeMillis() > entry.expiresAt) {
			otpStore.remove(email);
			return false;
		}
		boolean matches = entry.code.equals(code == null ? null : code.trim());
		if (matches) {
			otpStore.remove(email); // one-time use
		}
		return matches;
	}
}
