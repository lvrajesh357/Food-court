package com.currypoint.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceimp implements EmailService{

	 @Autowired
	    private JavaMailSender mailSender;

	@Override
	public void sendMail(String to, String subject, String body) {
		if (to == null || to.isBlank()) {
			return;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
			System.out.println("[Email] SENT to " + to);
		} catch (Exception e) {
			// Never let a mail failure (e.g. bad Gmail app password, SMTP down)
			// break checkout - the order is already placed and paid for.
			System.out.println("[Email] FAILED to " + to + " - " + e);
		}
	}

}
