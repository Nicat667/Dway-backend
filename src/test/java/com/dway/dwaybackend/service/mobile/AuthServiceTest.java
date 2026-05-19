package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.*;
import com.dway.dwaybackend.common.exception.verification.CodeRecentlySentException;
import com.dway.dwaybackend.common.exception.verification.InvalidVerificationCodeException;
import com.dway.dwaybackend.dto.request.auth.*;
import com.dway.dwaybackend.dto.response.auth.AuthResponse;
import com.dway.dwaybackend.dto.response.auth.RefreshTokenResponse;
import com.dway.dwaybackend.entity.EmailVerification;
import com.dway.dwaybackend.entity.PasswordResetToken;
import com.dway.dwaybackend.entity.RefreshToken;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.email.EmailService;
import com.dway.dwaybackend.repository.*;
import com.dway.dwaybackend.security.JwtUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailVerificationRepository emailVerificationRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks private AuthService authService;

    private static final UUID USER_ID     = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final String EMAIL     = "nicat@gmail.com";
    private static final String PASSWORD  = "password123";
    private static final String HASH      = "$2a$12$hashedPassword";
    private static final String NAME      = "Nicat";
    private static final String ACCESS_TOKEN = "eyJhbGci.access.token";
    private static final String RAW_REFRESH  = "raw-refresh-uuid";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "verificationExpiryMinutes", 15);
        ReflectionTestUtils.setField(authService, "refreshTokenExpirySeconds", 2592000L);
    }

    private User verifiedUser() {
        return User.builder()
                .id(USER_ID).name(NAME).email(EMAIL).password(HASH)
                .isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private User unverifiedUser() {
        return User.builder()
                .id(USER_ID).name(NAME).email(EMAIL).password(HASH)
                .isVerified(false).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private RegisterRequest registerRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setName(NAME);
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

    private EmailVerification validVerification(UUID userId) {
        return EmailVerification.builder()
                .id(UUID.randomUUID()).userId(userId)
                .code("847291")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
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

    // register()

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("saves user with hashed password and sends verification email")
        void withValidData_savesUserAndSendsEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

            authService.register(registerRequest());

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
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
            r.setEmail("Nicat@Gmail.COM");
            r.setPassword(PASSWORD);

            when(userRepository.existsByEmail("Nicat@Gmail.COM")).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any())).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

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
        @DisplayName("throws CodeRecentlySentException when code sent within 2 minutes")
        void whenCodeRecentlySent_throwsCodeRecentlySentException() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });

            EmailVerification recent = EmailVerification.builder()
                    .userId(USER_ID).code("123456")
                    .createdAt(LocalDateTime.now().minusSeconds(30))
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.of(recent));

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.register(registerRequest()));

            verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        }

        @Test
        @DisplayName("deletes old verification codes before creating new one")
        void withValidData_deletesOldCodesBeforeSaving() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", USER_ID);
                return u;
            });
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

            authService.register(registerRequest());

            verify(emailVerificationRepository).deleteAllByUserId(USER_ID);
            verify(emailVerificationRepository).save(any(EmailVerification.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // verifyEmail()
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        private VerifyEmailRequest request() {
            VerifyEmailRequest r = new VerifyEmailRequest();
            r.setEmail(EMAIL);
            r.setCode("847291");
            return r;
        }

        @Test
        @DisplayName("marks user verified and returns tokens on valid code")
        void withValidCode_marksVerifiedAndReturnsTokens() {
            User user = unverifiedUser();
            EmailVerification verification = validVerification(USER_ID);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(verification));
            when(jwtUtil.generateAccessToken(eq(USER_ID), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.verifyEmail(request());

            assertThat(user.isVerified()).isTrue();
            verify(userRepository).save(user);
            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotNull().isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("marks verification code as used")
        void withValidCode_marksCodeAsUsed() {
            User user = unverifiedUser();
            EmailVerification verification = validVerification(USER_ID);

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(verification));
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail(request());

            assertThat(verification.isUsed()).isTrue();
            verify(emailVerificationRepository).save(verification);
        }

        @Test
        @DisplayName("deletes all verification codes after successful verification")
        void withValidCode_deletesAllCodes() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(validVerification(USER_ID)));
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail(request());

            verify(emailVerificationRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void whenEmailNotFound_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> authService.verifyEmail(request()));
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code not found")
        void whenCodeNotFound_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.verifyEmail(request()));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code expired")
        void whenCodeExpired_throwsInvalidVerificationCodeException() {
            EmailVerification expired = EmailVerification.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).code("847291")
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .used(false)
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(expired));

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.verifyEmail(request()));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves refresh token with correct userId")
        void withValidCode_savesRefreshTokenWithCorrectUserId() {
            User user = unverifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(emailVerificationRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(validVerification(USER_ID)));
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail(request());

            RefreshToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTokenHash()).isNotNull().isNotBlank();
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // resendCode()
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resendCode()")
    class ResendCode {

        private ResendCodeRequest request() {
            ResendCodeRequest r = new ResendCodeRequest();
            r.setEmail(EMAIL);
            return r;
        }

        @Test
        @DisplayName("sends new code for unverified user")
        void withUnverifiedUser_sendsCode() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

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
        @DisplayName("throws CodeRecentlySentException when resent within 2 minutes")
        void whenCodeRecentlySent_throwsCodeRecentlySentException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(unverifiedUser()));

            EmailVerification recent = EmailVerification.builder()
                    .userId(USER_ID).code("123456")
                    .createdAt(LocalDateTime.now().minusSeconds(60))
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .build();
            when(emailVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.of(recent));

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.resendCode(request()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // login()
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("returns AuthResponse with tokens on valid credentials")
        void withValidCredentials_returnsAuthResponse() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(eq(USER_ID), any())).thenReturn(ACCESS_TOKEN);
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.login(loginRequest());

            assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(response.getRefreshToken()).isNotNull().isNotBlank();
            assertThat(response.getUser().getEmail()).isEqualTo(EMAIL);
            assertThat(response.getUser().getId()).isEqualTo(USER_ID);
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
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.login(loginRequest());

            String expectedHash = DigestUtils.md5DigestAsHex(response.getRefreshToken().getBytes());
            assertThat(captor.getValue().getTokenHash()).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("stores deviceInfo from request in refresh token")
        void withDeviceInfo_storesDeviceInfoInRefreshToken() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);
            when(jwtUtil.generateAccessToken(any(), any())).thenReturn(ACCESS_TOKEN);

            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            when(refreshTokenRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.login(loginRequest());

            assertThat(captor.getValue().getDeviceInfo()).isEqualTo("iPhone 14");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // refresh()
    // ══════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════
    // logout()
    // ══════════════════════════════════════════════════════════════════════

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

    // ══════════════════════════════════════════════════════════════════════
    // forgotPassword()
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        private ForgotPasswordRequest request() {
            ForgotPasswordRequest r = new ForgotPasswordRequest();
            r.setEmail(EMAIL);
            return r;
        }

        @Test
        @DisplayName("sends reset email when user exists")
        void whenUserExists_sendsResetEmail() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

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
        @DisplayName("throws CodeRecentlySentException when reset code sent within 2 minutes")
        void whenCodeRecentlySent_throwsCodeRecentlySentException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));

            PasswordResetToken recent = PasswordResetToken.builder()
                    .userId(USER_ID).code("123456")
                    .createdAt(LocalDateTime.now().minusSeconds(90))
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .used(false)
                    .build();
            when(passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.of(recent));

            assertThrows(CodeRecentlySentException.class,
                    () -> authService.forgotPassword(request()));

            verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        }

        @Test
        @DisplayName("deletes old reset tokens before saving new one")
        void withValidUser_deletesOldTokensFirst() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

            authService.forgotPassword(request());

            verify(passwordResetTokenRepository).deleteAllByUserId(USER_ID);
            verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        }

        @Test
        @DisplayName("saves reset token as unused with future expiry")
        void withValidUser_savesTokenWithCorrectState() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(Optional.empty());

            ArgumentCaptor<PasswordResetToken> captor =
                    ArgumentCaptor.forClass(PasswordResetToken.class);
            when(passwordResetTokenRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.forgotPassword(request());

            PasswordResetToken saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.isUsed()).isFalse();
            assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(saved.getCode()).hasSize(6).containsOnlyDigits();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // resetPassword()
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        private ResetPasswordRequest request() {
            ResetPasswordRequest r = new ResetPasswordRequest();
            r.setEmail(EMAIL);
            r.setCode("847291");
            r.setNewPassword("newPassword123");
            return r;
        }

        private PasswordResetToken validResetToken() {
            return PasswordResetToken.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).code("847291")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .used(false)
                    .build();
        }

        @Test
        @DisplayName("updates password and invalidates all sessions on success")
        void withValidCode_updatesPasswordAndInvalidatesSessions() {
            User user = verifiedUser();
            PasswordResetToken token = validResetToken();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            verify(userRepository).save(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("marks reset token as used")
        void withValidCode_marksTokenAsUsed() {
            User user = verifiedUser();
            PasswordResetToken token = validResetToken();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(token));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            assertThat(token.isUsed()).isTrue();
            verify(passwordResetTokenRepository).save(token);
        }

        @Test
        @DisplayName("deletes all reset tokens after successful reset")
        void withValidCode_deletesAllResetTokens() {
            User user = verifiedUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(validResetToken()));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            verify(passwordResetTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws UserNotFoundException when email not found")
        void whenEmailNotFound_throwsUserNotFoundException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> authService.resetPassword(request()));
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code not found")
        void whenCodeNotFound_throwsInvalidVerificationCodeException() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidVerificationCodeException.class,
                    () -> authService.resetPassword(request()));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws InvalidVerificationCodeException when code expired")
        void whenCodeExpired_throwsInvalidVerificationCodeException() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).code("847291")
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .used(false)
                    .build();

            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(verifiedUser()));
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(expired));

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
            when(passwordResetTokenRepository.findByUserIdAndCodeAndUsedFalse(USER_ID, "847291"))
                    .thenReturn(Optional.of(validResetToken()));
            when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$newHash");

            authService.resetPassword(request());

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            assertThat(user.getPassword()).doesNotContain("newPassword123");
        }
    }
}