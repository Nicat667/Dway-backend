package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.EmailAlreadyExistsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.common.exception.verification.InvalidVerificationCodeException;
import com.dway.dwaybackend.dto.request.auth.RegisterRequest;
import com.dway.dwaybackend.dto.request.auth.VerifyEmailRequest;
import com.dway.dwaybackend.dto.response.auth.AuthResponse;
import com.dway.dwaybackend.dto.response.auth.UserResponse;
import com.dway.dwaybackend.entity.EmailVerification;
import com.dway.dwaybackend.entity.RefreshToken;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.infrastructure.email.EmailService;
import com.dway.dwaybackend.repository.EmailVerificationRepository;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import com.dway.dwaybackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${app.email.verification-expiry-minutes:15}")
    private int verificationExpiryMinutes;

    @Value("${app.jwt.refresh-token-expiry:2592000}")
    private long refreshTokenExpirySeconds;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(hashedPassword)
                .build();

        userRepository.save(user);

        sendVerificationCode(user.getId(), request.getEmail(), request.getName());
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(UserNotFoundException::new);

        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndCodeAndUsedFalse(user.getId(), request.getCode())
                .orElseThrow(InvalidVerificationCodeException::new);

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidVerificationCodeException();
        }

        verification.setUsed(true);
        emailVerificationRepository.save(verification);

        user.setVerified(true);
        userRepository.save(user);

        emailVerificationRepository.deleteAllByUserId(user.getId());

        log.info("User {} verified email successfully", user.getId());

        return buildAuthResponse(user);
    }

    // ── Shared helpers

    public void sendVerificationCode(UUID userId, String email, String name) {
        emailVerificationRepository
                .findTopByUserIdOrderByCreatedAtDesc(userId)
                .ifPresent(existing -> {
                    boolean sentRecently = existing.getCreatedAt()
                            .isAfter(LocalDateTime.now().minusMinutes(2));
                    if (sentRecently) {
                        throw new CodeRecentlySentException();
                    }
                });

        String code = generateCode();

        EmailVerification verification = EmailVerification.builder()
                .userId(userId)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(verificationExpiryMinutes))
                .build();

        emailVerificationRepository.save(verification);
        emailService.sendVerificationEmail(email, name, code);
    }

    public AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoles());
        String rawRefreshToken = generateAndSaveRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    private String generateAndSaveRefreshToken(UUID userId) {

        String rawToken = UUID.randomUUID().toString();

        String tokenHash = org.springframework.util.DigestUtils
                .md5DigestAsHex(rawToken.getBytes());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now()
                        .plusSeconds(refreshTokenExpirySeconds))
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .plan(user.getPlan())
                .roles(user.getRoles())
                .points(user.getPoints())
                .streak(user.getStreak())
                .isVerified(user.isVerified())
                .build();
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}