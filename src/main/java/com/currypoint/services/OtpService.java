
package com.currypoint.services;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    private static final Logger log =
            LoggerFactory.getLogger(OtpService.class);

    private static final long OTP_VALID_MILLIS = 5 * 60 * 1000;

    private static class OtpEntry {
        final String code;
        final long expiresAt;

        OtpEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }

    private final Map<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    @Autowired
    private EmailService emailService;

    public String generateAndSend(String email) {

        log.info("OTP generation started for customer");

        String code = String.format("%06d",
                random.nextInt(1_000_000));

        otpStore.put(email,
                new OtpEntry(code,
                System.currentTimeMillis() + OTP_VALID_MILLIS));

        String subject = "Your FoodCourt login OTP";

        String message =
                "Your FoodCourt verification code is " + code +
                ". It expires in 5 minutes.\n\n" +
                "If you didn't request this, ignore this email.";

        log.info("OTP generated. Calling EmailService.");

        try {
            emailService.sendMail(email, subject, message);
            log.info("EmailService.sendMail() invoked.");
        } catch (Exception e) {
            log.error("Error invoking EmailService", e);
        }

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

        boolean matches =
                entry.code.equals(code == null ? null : code.trim());

        if (matches) {
            otpStore.remove(email);
        }

        return matches;
    }
}
