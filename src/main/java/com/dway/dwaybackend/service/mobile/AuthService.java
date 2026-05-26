package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.*;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.common.exception.verification.InvalidVerificationCodeException;
import com.dway.dwaybackend.dto.request.auth.*;
import com.dway.dwaybackend.dto.response.auth.AuthResponse;
import com.dway.dwaybackend.dto.response.auth.RefreshTokenResponse;
import com.dway.dwaybackend.entity.RefreshToken;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.infrastructure.email.EmailService;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import com.dway.dwaybackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String VERIFY_CODE_PREFIX     = "otp:verify:code:";
    private static final String VERIFY_COOLDOWN_PREFIX = "otp:verify:cooldown:";
    private static final String RESET_CODE_PREFIX      = "otp:reset:code:";
    private static final String RESET_COOLDOWN_PREFIX  = "otp:reset:cooldown:";
    private static final long   OTP_COOLDOWN_MINUTES   = 2;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.email.verification-expiry-minutes:15}")
    private int verificationExpiryMinutes;

    @Value("${app.jwt.refresh-token-expiry:2592000}")
    private long refreshTokenExpirySeconds;


    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        User user = User.builder()
                .name(request.getName())
                .surname(request.getSurname())
                .country(request.getCountry())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);
        sendVerificationCode(user.getId(), user.getEmail(), user.getName());
    }

    @Transactional
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElseThrow(UserNotFoundException::new);

        String storedCode = getCode(VERIFY_CODE_PREFIX + user.getId());

        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new InvalidVerificationCodeException();
        }

        user.setVerified(true);
        userRepository.save(user);

        redisTemplate.delete(VERIFY_CODE_PREFIX + user.getId());
        redisTemplate.delete(VERIFY_COOLDOWN_PREFIX + user.getId());

        log.info("User {} verified email successfully", user.getId());
        return buildAuthResponse(user, null);
    }

    @Transactional
    public void resendCode(ResendCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElseThrow(UserNotFoundException::new);

        if (user.isVerified()) {
            throw new EmailAlreadyVerifiedException();
        }

        sendVerificationCode(user.getId(), user.getEmail(), user.getName());
    }

    public void sendVerificationCode(UUID userId, String email, String name) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + userId))) {
            throw new CodeRecentlySentException();
        }

        String code = generateCode();

        redisTemplate.opsForValue().set(VERIFY_CODE_PREFIX + userId, code, verificationExpiryMinutes, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(VERIFY_COOLDOWN_PREFIX + userId, "1", OTP_COOLDOWN_MINUTES, TimeUnit.MINUTES);

        emailService.sendVerificationEmail(email, name, code);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase()).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isVerified()) {
            throw new EmailNotVerifiedException();
        }

        if (user.isBanned()) {
            throw new UserBannedException();
        }

        log.info("User {} logged in successfully", user.getId());
        return buildAuthResponse(user, request.getDeviceInfo());
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = DigestUtils.md5DigestAsHex(request.getRefreshToken().getBytes());

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash).orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidRefreshTokenException();
        }

        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(refreshToken.getUserId()).orElseThrow(UserNotFoundException::new);

        if (user.isBanned()) {
            throw new UserBannedException();
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoles());
        return RefreshTokenResponse.builder().accessToken(newAccessToken).build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = DigestUtils.md5DigestAsHex(request.getRefreshToken().getBytes());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshTokenRepository::delete);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().toLowerCase()).ifPresent(user -> {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(RESET_COOLDOWN_PREFIX + user.getId()))) {
                throw new CodeRecentlySentException();
            }

            String code = generateCode();

            redisTemplate.opsForValue().set(RESET_CODE_PREFIX + user.getId(), code, verificationExpiryMinutes, TimeUnit.MINUTES);
            redisTemplate.opsForValue().set(RESET_COOLDOWN_PREFIX + user.getId(), "1", OTP_COOLDOWN_MINUTES, TimeUnit.MINUTES);

            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), code);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(UserNotFoundException::new);

        String storedCode = getCode(RESET_CODE_PREFIX + user.getId());

        if (storedCode == null || !storedCode.equals(request.getCode())) {
            throw new InvalidVerificationCodeException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        refreshTokenRepository.deleteAllByUserId(user.getId());

        redisTemplate.delete(RESET_CODE_PREFIX + user.getId());
        redisTemplate.delete(RESET_COOLDOWN_PREFIX + user.getId());

        log.info("User {} reset password successfully", user.getId());
    }

    private String getCode(String key) {
        Object raw = redisTemplate.opsForValue().get(key);
        return raw != null ? (String) raw : null;
    }

    private AuthResponse buildAuthResponse(User user, String deviceInfo) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRoles());
        String rawRefreshToken = generateAndSaveRefreshToken(user.getId(), deviceInfo);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private String generateAndSaveRefreshToken(UUID userId, String deviceInfo) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = DigestUtils.md5DigestAsHex(rawToken.getBytes());

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpirySeconds))
                .build());

        return rawToken;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}