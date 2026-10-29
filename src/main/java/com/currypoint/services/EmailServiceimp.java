package com.currypoint.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceimp implements EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailServiceimp.class);

    private final JavaMailSender mailSender;

    public EmailServiceimp(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    @Async
    public void sendMail(String to, String subject, String body) {

        if (to == null || to.isBlank()) {
            log.warn("Email not sent because recipient is empty");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            log.info("Email sending completed");

        } catch (Exception e) {
            // Do not log OTPs, email passwords, or message bodies.
            log.error("Email sending failed: {}", e.getMessage());
        }
    }
}