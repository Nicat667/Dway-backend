package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.*;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.common.exception.verification.InvalidVerificationCodeException;
import com.dway.dwaybackend.dto.request.auth.*;
import com.dway.dwaybackend.dto.response.auth.AuthResponse;
import com.dway.dwaybackend.dto.response.auth.RefreshTokenResponse;
import com.dway.dwaybackend.dto.response.auth.UserResponse;
import com.dway.dwaybackend.entity.RefreshToken;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Country;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.email.EmailService;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import com.dway.dwaybackend.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserMapper userMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    @InjectMocks private AuthService authService;

    private static final UUID   USER_ID      = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final String EMAIL        = "nicat@gmail.com";
    private static final String PASSWORD     = "password123";
    private static final String HASH         = "$2a$12$hashedPassword";
    private static final String NAME         = "Nicat";
    private static final String SURNAME      = "Mammadov";
    private static final Country COUNTRY      = Country.AZERBAIJAN;
    private static final String ACCESS_TOKEN = "eyJhbGci.access.token";
    private static final String RAW_REFRESH  = "raw-refresh-uuid";
    private static final String OTP_CODE     = "847291";

    private static final String VERIFY_CODE_PREFIX     = "otp:verify:code:";
    private static final String VERIFY_COOLDOWN_PREFIX = "otp:verify:cooldown:";
    private static final String RESET_CODE_PREFIX      = "otp:reset:code:";
    private static final String RESET_COOLDOWN_PREFIX  = "otp:reset:cooldown:";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationExpiryMinutes", 15);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirySeconds", 2592000L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ------------------------------------------------------------------ helpers

    private User verifiedUser() {
        return User.builder()
                .id(USER_ID).name(NAME).surname(SURNAME).country(COUNTRY).email(EMAIL).password(HASH)
                .isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private User unverifiedUser() {
        return User.builder()
                .id(USER_ID).name(NAME).surname(SURNAME).country(COUNTRY).email(EMAIL).password(HASH)
                .isVerified(false).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(USER_ID).name(NAME).surname(SURNAME).country(COUNTRY).email(EMAIL)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private RefreshToken validRefreshToken(UUID userId) {
        return RefreshToken.builder()
                .id(UUID.randomUUID()).userId(userId)
                .tokenHash(DigestUtils.md5DigestAsHex(RAW_REFRESH.getBytes()))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setName(NAME);
        r.setSurname(SURNAME);
        r.setCountry(COUNTRY);
        r.setEmail(EMAIL);
        r.setPassword(PASSWORD);
        return r;
    }

    private LoginRequest loginRequest() {
        LoginRequest r = new LoginRequest();
        r.setEmail(EMAIL);
        r.setPassword(PASSWORD);
        r.setDeviceInfo("iPhone 14");
        return r;
    }

    // ================================================================== register()

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("saves user with name, surname, hashed password and sends verification email")
        void withValidData_savesUserAndSendsEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.register(registerRequest());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getName()).isEqualTo(NAME);
            assertThat(userCaptor.getValue().getSurname()).isEqualTo(SURNAME);
            assertThat(userCaptor.getValue().getCountry()).isEqualTo(COUNTRY);
            assertThat(userCaptor.getValue().getEmail()).isEqualTo(EMAIL);
            assertThat(userCaptor.getValue().getPassword()).isEqualTo(HASH);
            assertThat(userCaptor.getValue().isVerified()).isFalse();
            verify(emailService).sendVerificationEmail(eq(EMAIL), eq(NAME), anyString());
        }

        @Test
        @DisplayName("saves email as lowercase regardless of input")
        void withMixedCaseEmail_savesAsLowercase() {
            RegisterRequest r = new RegisterRequest();
            r.setName(NAME);
            r.setSurname(SURNAME);
            r.setCountry(COUNTRY);
            r.setEmail("Nicat@Gmail.COM");
            r.setPassword(PASSWORD);

            when(userRepository.existsByEmail("Nicat@Gmail.COM")).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.register(r);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("nicat@gmail.com");
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email taken")
        void whenEmailTaken_throwsEmailAlreadyExistsException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThrows(EmailAlreadyExistsException.class,
                    () -> authService.register(registerRequest()));

            verify(userRepository, never()).save(any());
            verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("throws CodeRecentlySentException when cooldown key still alive")
        void whenCodeRecentlySent_throwsCodeRecentlySentException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(true);

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.register(registerRequest()));

            verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("stores OTP and cooldown in Redis with correct TTLs")
        void withValidData_storesOtpInRedisWithTtl() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.register(registerRequest());

            verify(valueOps).set(eq(VERIFY_CODE_PREFIX + USER_ID), anyString(), eq(15L), eq(TimeUnit.MINUTES));
            verify(valueOps).set(eq(VERIFY_COOLDOWN_PREFIX + USER_ID), eq("1"), eq(2L), eq(TimeUnit.MINUTES));
        }
    }

    // ================================================================== verifyEmail()

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        private VerifyEmailRequest request() {
            VerifyEmailRequest r = new VerifyEmailRequest();
            r.setEmail(EMAIL);
            r.setCode(OTP_CODE);
            return r;
        }

        @Test
        @DisplayName("marks user verified and returns tokens on valid code")
        void withValidCode_marksVerifiedAndReturnsTokens() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(jwtUtil.generateAccessToken(eq(USER_ID), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            AuthResponse response = authService.verifyEmail(request());

            assertThat(user.isVerified()).isTrue();
            verify(userRepository).save(user);
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotNull().isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
            assertThat(response.getUser().getSurname()).isEqualTo(SURNAME);
        }

        @Test
        @DisplayName("saves user to DB before deleting Redis keys")
        void withValidCode_savesUserBeforeDeletingRedisKeys() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            InOrder order = inOrder(userRepository, redisTemplate);

            authService.verifyEmail(request());

            order.verify(userRepository).save(user);
            order.verify(redisTemplate).delete(VERIFY_CODE_PREFIX + USER_ID);
            order.verify(redisTemplate).delete(VERIFY_COOLDOWN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("deletes both Redis keys after successful verification")
        void withValidCode_deletesBothRedisKeys() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            authService.verifyEmail(request());

            verify(redisTemplate).delete(VERIFY_CODE_PREFIX + USER_ID);
            verify(redisTemplate).delete(VERIFY_COOLDOWN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("saves refresh token with correct userId")
        void withValidCode_savesRefreshTokenWithCorrectUserId() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail(request());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTokenHash()).isNotNull().isNotBlank();
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void whenEmailNotFound_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.verifyEmail(request()));
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when Redis key is absent")
        void whenCodeAbsentInRedis_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn(null);

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.verifyEmail(request()));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code does not match")
        void whenCodeMismatch_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(valueOps.get(VERIFY_CODE_PREFIX + USER_ID)).thenReturn("999999");

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.verifyEmail(request()));

            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================== resendCode()

    @Nested
    @DisplayName("resendCode()")
    class ResendCode {

        private ResendCodeRequest request() {
            ResendCodeRequest r = new ResendCodeRequest();
            r.setEmail(EMAIL);
            return r;
        }

        @Test
        @DisplayName("sends new code for unverified user when no cooldown active")
        void withUnverifiedUser_sendsCode() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.resendCode(request());

            verify(emailService).sendVerificationEmail(eq(EMAIL), eq(NAME), anyString());
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void whenEmailNotFound_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.resendCode(request()));
        }

        @Test
        @DisplayName("throws EmailAlreadyVerifiedException when user already verified")
        void whenAlreadyVerified_throwsEmailAlreadyVerifiedException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));

            assertThrows(EmailAlreadyVerifiedException.class,
                    () -> authService.resendCode(request()));

            verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("throws CodeRecentlySentException when cooldown key still alive")
        void whenCooldownActive_throwsCodeRecentlySentException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(redisTemplate.hasKey(VERIFY_COOLDOWN_PREFIX + USER_ID)).thenReturn(true);

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.resendCode(request()));

            verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        }
    }

    // ================================================================== login()

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns AuthResponse with tokens on valid credentials")
        void withValidCredentials_returnsAuthResponse() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(eq(USER_ID), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            AuthResponse response = authService.login(loginRequest());

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotNull().isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
            assertThat(response.getUser().getSurname()).isEqualTo(SURNAME);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when email not found")
        void whenEmailNotFound_throwsInvalidCredentials() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(loginRequest()));

            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when password wrong")
        void whenPasswordWrong_throwsInvalidCredentials() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(false);

            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(loginRequest()));

            verify(jwtUtil, never()).generateAccessToken(any(), any());
            verify(refreshTokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws EmailNotVerifiedException when user not verified")
        void whenNotVerified_throwsEmailNotVerifiedException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

            assertThrows(EmailNotVerifiedException.class,
                    () -> authService.login(loginRequest()));

            verify(jwtUtil, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("throws UserBannedException when user is banned")
        void whenBanned_throwsUserBannedException() {
            User banned = verifiedUser();
            banned.setBanned(true);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(banned));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

            assertThrows(UserBannedException.class, () -> authService.login(loginRequest()));

            verify(jwtUtil, never()).generateAccessToken(any(), any());
        }

        @Test
        @DisplayName("stores refresh token hash not raw token")
        void withValidCredentials_storesHashNotRawToken() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.login(loginRequest());

            String expectedHash = DigestUtils.md5DigestAsHex(response.getRefreshToken().getBytes());
            assertThat(captor.getValue().getTokenHash()).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("stores deviceInfo from request in refresh token")
        void withDeviceInfo_storesDeviceInfoInRefreshToken() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(userMapper.toUserResponse(user)).thenReturn(userResponse());

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            authService.login(loginRequest());

            assertThat(captor.getValue().getDeviceInfo()).isEqualTo("iPhone 14");
        }
    }

    // ================================================================== refresh()

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        private RefreshTokenRequest request() {
            RefreshTokenRequest r = new RefreshTokenRequest();
            r.setRefreshToken(RAW_REFRESH);
            return r;
        }

        @Test
        @DisplayName("returns new access token when refresh token valid")
        void withValidToken_returnsNewAccessToken() {
            when(refreshTokenRepository.findByTokenHash(
                    DigestUtils.md5DigestAsHex(RAW_REFRESH.getBytes())))
                    .thenReturn(Optional.of(validRefreshToken(USER_ID)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(verifiedUser()));
            when(jwtUtil.generateAccessToken(eq(USER_ID), any())).thenReturn(ACCESS_TOKEN);

            RefreshTokenResponse response = authService.refresh(request());

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException when token not found")
        void whenTokenNotFound_throwsInvalidRefreshTokenException() {
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertThrows(InvalidRefreshTokenException.class,
                    () -> authService.refresh(request()));
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException and deletes when token expired")
        void whenTokenExpired_deletesAndThrowsInvalidRefreshTokenException() {
            RefreshToken expired = RefreshToken.builder()
                    .id(UUID.randomUUID()).userId(USER_ID)
                    .tokenHash(DigestUtils.md5DigestAsHex(RAW_REFRESH.getBytes()))
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

            assertThrows(InvalidRefreshTokenException.class,
                    () -> authService.refresh(request()));

            verify(refreshTokenRepository).delete(expired);
        }

        @Test
        @DisplayName("throws InvalidRefreshTokenException when token revoked")
        void whenTokenRevoked_throwsInvalidRefreshTokenException() {
            RefreshToken revoked = validRefreshToken(USER_ID);
            revoked.setRevoked(true);

            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

            assertThrows(InvalidRefreshTokenException.class,
                    () -> authService.refresh(request()));
        }

        @Test
        @DisplayName("throws UserBannedException when user is banned")
        void whenUserBanned_throwsUserBannedException() {
            User banned = verifiedUser();
            banned.setBanned(true);

            when(refreshTokenRepository.findByTokenHash(any()))
                    .thenReturn(Optional.of(validRefreshToken(USER_ID)));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(banned));

            assertThrows(UserBannedException.class, () -> authService.refresh(request()));

            verify(jwtUtil, never()).generateAccessToken(any(), any());
        }
    }

    // ================================================================== logout()

    @Nested
    @DisplayName("logout()")
    class Logout {

        private LogoutRequest request() {
            LogoutRequest r = new LogoutRequest();
            r.setRefreshToken(RAW_REFRESH);
            return r;
        }

        @Test
        @DisplayName("deletes refresh token when found")
        void whenTokenFound_deletesToken() {
            RefreshToken token = validRefreshToken(USER_ID);
            when(refreshTokenRepository.findByTokenHash(
                    DigestUtils.md5DigestAsHex(RAW_REFRESH.getBytes())))
                    .thenReturn(Optional.of(token));

            authService.logout(request());

            verify(refreshTokenRepository).delete(token);
        }

        @Test
        @DisplayName("does nothing when token not found — idempotent")
        void whenTokenNotFound_doesNothing() {
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> authService.logout(request()));

            verify(refreshTokenRepository, never()).delete(any());
        }

        @Test
        @DisplayName("looks up by MD5 hash of the raw token")
        void lookupsByHashNotRaw() {
            String expectedHash = DigestUtils.md5DigestAsHex(RAW_REFRESH.getBytes());
            when(refreshTokenRepository.findByTokenHash(expectedHash)).thenReturn(Optional.empty());

            authService.logout(request());

            verify(refreshTokenRepository).findByTokenHash(expectedHash);
            verify(refreshTokenRepository, never()).findByTokenHash(RAW_REFRESH);
        }
    }

    // ================================================================== forgotPassword()

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        private ForgotPasswordRequest request() {
            ForgotPasswordRequest r = new ForgotPasswordRequest();
            r.setEmail(EMAIL);
            return r;
        }

        @Test
        @DisplayName("sends reset email when user exists and no cooldown active")
        void whenUserExists_sendsResetEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(redisTemplate.hasKey(RESET_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.forgotPassword(request());

            verify(emailService).sendPasswordResetEmail(eq(EMAIL), eq(NAME), anyString());
        }

        @Test
        @DisplayName("returns silently when email not found — prevents enumeration")
        void whenEmailNotFound_returnsWithoutException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertDoesNotThrow(() -> authService.forgotPassword(request()));

            verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        }

        @Test
        @DisplayName("throws CodeRecentlySentException when cooldown key still alive")
        void whenCooldownActive_throwsCodeRecentlySentException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(redisTemplate.hasKey(RESET_COOLDOWN_PREFIX + USER_ID)).thenReturn(true);

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.forgotPassword(request()));

            verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        }

        @Test
        @DisplayName("stores reset OTP and cooldown in Redis with correct TTLs")
        void withValidUser_storesResetOtpInRedisWithTtl() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(redisTemplate.hasKey(RESET_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            authService.forgotPassword(request());

            verify(valueOps).set(eq(RESET_CODE_PREFIX + USER_ID), anyString(), eq(15L), eq(TimeUnit.MINUTES));
            verify(valueOps).set(eq(RESET_COOLDOWN_PREFIX + USER_ID), eq("1"), eq(2L), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("stored reset code is 6 digits")
        void withValidUser_storesSixDigitCode() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(redisTemplate.hasKey(RESET_COOLDOWN_PREFIX + USER_ID)).thenReturn(false);

            ArgumentCaptor<Object> codeCaptor = ArgumentCaptor.forClass(Object.class);
            doNothing().when(valueOps).set(eq(RESET_CODE_PREFIX + USER_ID),
                    codeCaptor.capture(), anyLong(), any());

            authService.forgotPassword(request());

            assertThat(codeCaptor.getValue().toString()).hasSize(6).containsOnlyDigits();
        }
    }

    // ================================================================== resetPassword()

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        private ResetPasswordRequest request() {
            ResetPasswordRequest r = new ResetPasswordRequest();
            r.setEmail(EMAIL);
            r.setCode(OTP_CODE);
            r.setNewPassword("newPassword123");
            return r;
        }

        @Test
        @DisplayName("updates password and invalidates all sessions on success")
        void withValidCode_updatesPasswordAndInvalidatesSessions() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            verify(userRepository).save(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("saves user and deletes sessions in DB before deleting Redis keys")
        void withValidCode_dbOperationsBeforeRedisCleanup() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            InOrder order = inOrder(userRepository, refreshTokenRepository, redisTemplate);

            authService.resetPassword(request());

            order.verify(userRepository).save(user);
            order.verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
            order.verify(redisTemplate).delete(RESET_CODE_PREFIX + USER_ID);
            order.verify(redisTemplate).delete(RESET_COOLDOWN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("deletes both Redis keys after successful reset")
        void withValidCode_deletesBothRedisKeys() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            verify(redisTemplate).delete(RESET_CODE_PREFIX + USER_ID);
            verify(redisTemplate).delete(RESET_COOLDOWN_PREFIX + USER_ID);
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void whenEmailNotFound_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> authService.resetPassword(request()));
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when Redis key absent")
        void whenCodeAbsentInRedis_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn(null);

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.resetPassword(request()));

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code does not match")
        void whenCodeMismatch_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn("000000");

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.resetPassword(request()));

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("hashes new password before saving")
        void withValidCode_hashesNewPassword() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(valueOps.get(RESET_CODE_PREFIX + USER_ID)).thenReturn(OTP_CODE);
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            assertThat(user.getPassword()).doesNotContain("newPassword123");
        }
    }
}