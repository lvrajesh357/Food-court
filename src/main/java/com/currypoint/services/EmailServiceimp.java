package com.currypoint.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * Sends e-mail without ever making the customer wait: sendMail() is @Async, so the
 * web request returns immediately and the mail goes out on a background thread.
 *
 * TWO ways to send (picked automatically):
 *
 *  1. HTTPS API (Brevo)  - used when the BREVO_API_KEY environment variable is set.
 *     This is what you need on Render's FREE plan: Render blocks outbound SMTP
 *     ports (25 / 465 / 587) there, so Gmail SMTP can never connect. An HTTPS API
 *     call uses port 443, which is always open, and is also much faster than SMTP.
 *
 *  2. SMTP (Gmail etc.)  - fallback when no API key is set. Works on your own PC
 *     and on paid hosts that allow SMTP. Timeouts are configured in
 *     application.properties so a blocked port fails in seconds instead of hanging.
 */
@Service
public class EmailServiceimp implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceimp.class);

    private static final int MAX_ATTEMPTS = 3;

    private final JavaMailSender mailSender;

    // One shared client: connection reuse makes the 2nd, 3rd... mail noticeably faster.
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.brevo-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoUrl;

    /** Sender address. For Brevo this MUST be a sender you verified in the Brevo dashboard. */
    @Value("${app.mail.from:}")
    private String fromEmail;

    @Value("${app.mail.from-name:FoodCourt}")
    private String fromName;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${app.owner-email:}")
    private String ownerEmail;

    public EmailServiceimp(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    void logMailSetup() {
        // The two most common reasons "no mail arrives": nobody to send the owner alert to, and a
        // Gmail/Yahoo sender used through Brevo (those providers often spam-fold or drop it).
        if (isBlank(ownerEmail)) {
            log.warn("MAIL: OWNER_EMAIL is not set - the 'New FoodCourt Order' mail to the owner will NOT be sent.");
        }
        String sender = senderAddress().toLowerCase();
        if (useBrevo() && (sender.endsWith("@gmail.com") || sender.endsWith("@yahoo.com") || sender.endsWith("@outlook.com"))) {
            log.warn("MAIL: sender {} is a free-mail address. Brevo accepts it, but Gmail/Yahoo often put such mails in "
                    + "Spam or reject them (the domain is not yours). Check Spam first; for reliable delivery verify "
                    + "your own domain in Brevo.", sender);
        }
        if (useBrevo()) {
            log.info("MAIL: using Brevo HTTPS API (sender: {}). Works on Render free plan.", senderAddress());
        } else {
            boolean onRender = System.getenv("RENDER") != null;
            log.info("MAIL: using SMTP (account: {}).", isBlank(smtpUsername) ? "NOT SET" : smtpUsername);
            if (onRender) {
                log.warn("MAIL: running on Render with SMTP. Render's free plan BLOCKS SMTP ports 25/465/587, "
                        + "so mails will fail. Set BREVO_API_KEY (and MAIL_FROM) to send over HTTPS instead.");
            }
        }
    }

    @Override
    @Async
    public void sendMail(String to, String subject, String body) {
        if (isBlank(to)) {
            log.warn("Email not sent because recipient is empty");
            return;
        }
        String recipient = to.trim();
        long start = System.currentTimeMillis();
        String via = useBrevo() ? "Brevo API" : "SMTP";

        boolean sent;
        try {
            sent = useBrevo() ? sendViaBrevo(recipient, subject, body) : sendViaSmtp(recipient, subject, body);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}", mask(recipient), e);
            sent = false;
        }

        long took = System.currentTimeMillis() - start;
        if (sent) {
            log.info("Email sent to {} via {} in {} ms", mask(recipient), via, took);
        } else {
            log.error("Email to {} FAILED via {} after {} ms - see the warning/error lines just above for the reason.",
                    mask(recipient), via, took);
        }
    }

    // ------------------------------------------------------------------
    // Brevo HTTPS API
    // ------------------------------------------------------------------

    private boolean useBrevo() {
        return !isBlank(brevoApiKey);
    }

    private String senderAddress() {
        return !isBlank(fromEmail) ? fromEmail.trim() : (smtpUsername == null ? "" : smtpUsername.trim());
    }

    private boolean sendViaBrevo(String to, String subject, String body) {
        String sender = senderAddress();
        if (sender.isEmpty()) {
            log.error("Brevo needs a sender address: set MAIL_FROM to an email you verified in Brevo.");
            return false;
        }

        String json = buildBrevoJson(sender, isBlank(fromName) ? "FoodCourt" : fromName, to, subject, body);
        HttpRequest request = HttpRequest.newBuilder(URI.create(brevoUrl))
                .timeout(Duration.ofSeconds(10))
                .header("accept", "application/json")
                .header("content-type", "application/json")
                .header("api-key", brevoApiKey.trim())
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = response.statusCode();
                if (code >= 200 && code < 300) {
                    return true;
                }
                log.warn("Brevo rejected the email (HTTP {}), attempt {}/{}: {}", code, attempt, MAX_ATTEMPTS,
                        shorten(response.body()));
                // 4xx (except 429) = wrong API key / sender not verified / bad address.
                // Retrying will not fix that, so stop straight away.
                if (code < 500 && code != 429) {
                    if (code == 401) {
                        log.error("Brevo says the API key is invalid. Check BREVO_API_KEY.");
                    } else if (code == 400 || code == 403) {
                        log.error("Brevo refused the request. Most often the sender '{}' is not verified in Brevo "
                                + "(Brevo -> Senders, Domains & Dedicated IPs -> Senders).", sender);
                    }
                    return false;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            } catch (IOException e) {
                log.warn("Brevo request failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.toString());
            }

            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(400L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /** Builds the JSON body for POST /v3/smtp/email (plain-text mail). Package-private for testing. */
    static String buildBrevoJson(String senderEmail, String senderName, String to, String subject, String text) {
        StringBuilder sb = new StringBuilder(256 + (text == null ? 0 : text.length()));
        sb.append("{\"sender\":{\"name\":").append(quote(senderName))
          .append(",\"email\":").append(quote(senderEmail)).append("},")
          .append("\"to\":[{\"email\":").append(quote(to)).append("}],")
          .append("\"subject\":").append(quote(subject)).append(',')
          .append("\"textContent\":").append(quote(text))
          .append('}');
        return sb.toString();
    }

    /** Turns a Java string into a valid JSON string literal (with the surrounding quotes). */
    static String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // SMTP fallback (Gmail etc.)
    // ------------------------------------------------------------------

    private boolean sendViaSmtp(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (!isBlank(smtpUsername)) {
                message.setFrom(smtpUsername.trim());
            }
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("SMTP send failed: {}", rootMessage(e));
            if (System.getenv("RENDER") != null) {
                log.error("Render's free plan blocks SMTP. Set BREVO_API_KEY + MAIL_FROM to send over HTTPS.");
            }
            return false;
        }
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** ra***@gmail.com - keeps full addresses out of the logs. */
    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return email;
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
