package com.dway.dwaybackend.infrastructure.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async("emailExecutor")
    public void sendVerificationEmail(String toEmail, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Verify your Dway account");
            message.setText(buildVerificationText(name, code));
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendPasswordResetEmail(String toEmail, String name, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Reset your Dway password");
            message.setText(buildPasswordResetText(name, code));
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildVerificationText(String name, String code) {
        return String.format("""
                Hi %s,
                
                Welcome to Dway! Please verify your email address.
                
                Your verification code is: %s
                
                This code expires in 15 minutes.
                
                If you did not create a Dway account, ignore this email.
                
                The Dway Team
                """, name, code);
    }

    private String buildPasswordResetText(String name, String code) {
        return String.format("""
                Hi %s,
                
                You requested to reset your Dway password.
                
                Your reset code is: %s
                
                This code expires in 15 minutes.
                
                If you did not request a password reset, ignore this email.
                Your password will not be changed.
                
                The Dway Team
                """, name, code);
    }
}