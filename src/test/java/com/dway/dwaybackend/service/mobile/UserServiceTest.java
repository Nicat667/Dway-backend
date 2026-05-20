package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.InvalidCredentialsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.ChangePasswordRequest;
import com.dway.dwaybackend.dto.request.user.UpdateProfileRequest;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private S3Client s3Client;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    private static final UUID USER_ID = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final String EMAIL   = "nicat@gmail.com";
    private static final String NAME    = "Nicat";
    private static final String HASH    = "$2a$12$hashedPassword";
    private static final String BUCKET  = "dway-backend";
    private static final String REGION  = "us-east-1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "bucket", BUCKET);
        ReflectionTestUtils.setField(userService, "region", REGION);
    }

    private User user() {
        return User.builder()
                .id(USER_ID).name(NAME).email(EMAIL).password(HASH)
                .isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private UserProfileResponse profileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId()).name(user.getName()).email(user.getEmail())
                .avatarUrl(user.getAvatarUrl()).country(user.getCountry())
                .plan(user.getPlan()).roles(user.getRoles())
                .points(user.getPoints()).streak(user.getStreak())
                .isVerified(user.isVerified())
                .build();
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("returns profile for existing user")
        void withValidUser_returnsProfile() {
            User user = user();
            UserProfileResponse response = profileResponse(user);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(response);

            UserProfileResponse result = userService.getMyProfile(USER_ID);

            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            verify(userMapper).toProfileResponse(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.getMyProfile(USER_ID));

            verify(userMapper, never()).toProfileResponse(any());
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("updates only name when only name is provided")
        void withNameOnly_updatesName() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setName("New Name");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getName()).isEqualTo("New Name");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("updates only country when only country is provided")
        void withCountryOnly_updatesCountry() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setCountry("Azerbaijan");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getCountry()).isEqualTo("Azerbaijan");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("updates push token when provided")
        void withPushToken_updatesPushToken() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setPushToken("fcm-token-xyz");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getPushToken()).isEqualTo("fcm-token-xyz");
        }

        @Test
        @DisplayName("does not overwrite existing fields when request fields are null")
        void withNullFields_doesNotOverwriteExistingValues() {
            User user = user();
            user.setCountry("Turkey");
            UpdateProfileRequest request = new UpdateProfileRequest();
            // name, country, pushToken all null

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getCountry()).isEqualTo("Turkey");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateProfile(USER_ID, new UpdateProfileRequest()));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("uploadAvatar()")
    class UploadAvatar {

        private MockMultipartFile validJpeg() {
            return new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[1024]);
        }

        @Test
        @DisplayName("uploads file to S3 and saves URL on user")
        void withValidFile_uploadsAndSavesUrl() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
            verify(userRepository).save(user);
            assertThat(user.getAvatarUrl()).contains(BUCKET).contains("avatars/" + USER_ID);
        }

        @Test
        @DisplayName("deletes old avatar from S3 before uploading new one")
        void whenUserHasExistingAvatar_deletesOldFirst() {
            User user = user();
            user.setAvatarUrl("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/avatars/" + USER_ID + "/old.jpg");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("does not call deleteObject when user has no existing avatar")
        void whenUserHasNoAvatar_doesNotCallDelete() {
            User user = user(); // avatarUrl is null

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("400 when file is empty")
        void whenFileIsEmpty_throws400() {
            MockMultipartFile empty = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.uploadAvatar(USER_ID, empty));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(userRepository, never()).findById(any());
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("400 when file is not an image")
        void whenFileIsNotImage_throws400() {
            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[1024]);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.uploadAvatar(USER_ID, pdf));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("400 when file exceeds 5 MB")
        void whenFileTooLarge_throws400() {
            MockMultipartFile large = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.uploadAvatar(USER_ID, large));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("constructed avatar URL contains bucket, region, and userId path")
        void urlContainsBucketRegionAndUserPath() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(user)).thenReturn(user);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            assertThat(user.getAvatarUrl())
                    .startsWith("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/avatars/" + USER_ID + "/")
                    .endsWith(".jpg");
        }
    }

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        private ChangePasswordRequest request(String current, String next) {
            ChangePasswordRequest r = new ChangePasswordRequest();
            r.setCurrentPassword(current);
            r.setNewPassword(next);
            return r;
        }

        @Test
        @DisplayName("updates password and invalidates all sessions on success")
        void withCorrectPassword_updatesAndInvalidatesSessions() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", HASH)).thenReturn(true);
            when(passwordEncoder.encode("newPass123!")).thenReturn("$2a$newHash");

            userService.changePassword(USER_ID, request("oldPass123", "newPass123!"));

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            verify(userRepository).save(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when current password is wrong")
        void whenCurrentPasswordWrong_throwsInvalidCredentials() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPass", HASH)).thenReturn(false);

            assertThrows(InvalidCredentialsException.class,
                    () -> userService.changePassword(USER_ID, request("wrongPass", "newPass123!")));

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.changePassword(USER_ID, request("any", "newPass123!")));
        }

        @Test
        @DisplayName("stores encoded password, never plaintext")
        void withCorrectPassword_storesEncodedPassword() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", HASH)).thenReturn(true);
            when(passwordEncoder.encode("newPass123!")).thenReturn("$2a$newHash");

            userService.changePassword(USER_ID, request("oldPass123", "newPass123!"));

            assertThat(user.getPassword()).doesNotContain("newPass123!");
            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
        }
    }

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccount {

        @Test
        @DisplayName("deletes user and invalidates all sessions")
        void withValidUser_deletesUserAndSessions() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("deletes avatar from S3 before deleting user")
        void whenUserHasAvatar_deletesAvatarFromS3First() {
            User user = user();
            user.setAvatarUrl("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/avatars/" + USER_ID + "/avatar.jpg");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            InOrder order = inOrder(s3Client, userRepository);
            order.verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
            order.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("does not call S3 delete when user has no avatar")
        void whenUserHasNoAvatar_skipsS3Delete() {
            User user = user(); // avatarUrl is null

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.deleteAccount(USER_ID));

            verify(userRepository, never()).delete(any());
        }
    }
}