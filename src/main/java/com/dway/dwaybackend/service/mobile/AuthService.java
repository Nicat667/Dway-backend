package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.EmailAlreadyExistsException;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.dto.request.auth.RegisterRequest;
import com.dway.dwaybackend.entity.EmailVerification;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.infrastructure.email.EmailService;
import com.dway.dwaybackend.repository.EmailVerificationRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.email.verification-expiry-minutes:15}")
    private int verificationExpiryMinutes;

    @Transactional
    public void register(RegisterRequest request) {
        // Check email not already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        // Hash password — BCrypt, cost factor 12
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // Save user — not verified yet
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(hashedPassword)
                .build();

        userRepository.save(user);

        // Send verification code
        sendVerificationCode(user.getId(), request.getEmail(), request.getName());
    }

    // Reusable — called by register and resend-code
    public void sendVerificationCode(java.util.UUID userId, String email, String name) {
        // Prevent spamming — check if code was recently sent
        emailVerificationRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(existing -> {
                    boolean sentRecently = existing.getCreatedAt()
                            .isAfter(LocalDateTime.now().minusMinutes(2));
                    if (sentRecently) {
                        throw new CodeRecentlySentException();
                    }
                });

        // Generate 6-digit code
        String code = generateCode();

        // Save verification record
        EmailVerification verification = EmailVerification.builder()
                .userId(userId)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationExpiryMinutes))
                .build();

        emailVerificationRepository.save(verification);

        // Send email async — does not block the response
        emailService.sendVerificationEmail(email, name, code);
    }

    private String generateCode() {
        // SecureRandom — cryptographically strong, not predictable
        // Regular Random is predictable — attacker could guess next code
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000); // always 6 digits
        return String.valueOf(code);
    }
}