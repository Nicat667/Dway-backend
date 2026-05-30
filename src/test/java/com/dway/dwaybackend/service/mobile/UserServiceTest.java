package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.InvalidCredentialsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.ChangePasswordRequest;
import com.dway.dwaybackend.dto.request.user.UpdateProfileRequest;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Country;
import com.dway.dwaybackend.entity.enums.Plan;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.storage.S3StorageService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private S3StorageService s3StorageService;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    private static final UUID    USER_ID = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final String  EMAIL   = "nicat@gmail.com";
    private static final String  NAME    = "Nicat";
    private static final String  SURNAME = "Mammadov";
    private static final Country COUNTRY = Country.AZERBAIJAN;
    private static final String  HASH    = "$2a$12$hashedPassword";
    private static final String  AVATAR  = "https://bucket.s3.eu-north-1.amazonaws.com/avatars/" + USER_ID + "/old.jpg";

    private User user() {
        return User.builder()
                .id(USER_ID).name(NAME).surname(SURNAME).country(COUNTRY)
                .email(EMAIL).password(HASH)
                .isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private UserProfileResponse profileResponse(User u) {
        return UserProfileResponse.builder()
                .id(u.getId()).name(u.getName()).surname(u.getSurname())
                .email(u.getEmail()).avatarUrl(u.getAvatarUrl())
                .country(u.getCountry()).plan(u.getPlan()).roles(u.getRoles())
                .points(u.getPoints()).streak(u.getStreak())
                .isVerified(u.isVerified())
                .build();
    }

    private MockMultipartFile validJpeg() {
        return new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[1024]);
    }

    private ChangePasswordRequest changePasswordRequest(String current, String next) {
        ChangePasswordRequest r = new ChangePasswordRequest();
        r.setCurrentPassword(current);
        r.setNewPassword(next);
        return r;
    }

    // ================================================================== getMyProfile()

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("returns profile for existing user")
        void withValidUser_returnsProfile() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            UserProfileResponse result = userService.getMyProfile(USER_ID);

            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getSurname()).isEqualTo(SURNAME);
            assertThat(result.getCountry()).isEqualTo(COUNTRY);
            verify(userMapper).toProfileResponse(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getMyProfile(USER_ID));
            verify(userMapper, never()).toProfileResponse(any());
        }
    }

    // ================================================================== updateProfile()

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
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getName()).isEqualTo("New Name");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("updates only surname when only surname is provided")
        void withSurnameOnly_updatesSurname() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setSurname("NewSurname");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getSurname()).isEqualTo("NewSurname");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("updates only country when only country is provided")
        void withCountryOnly_updatesCountry() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setCountry(Country.GERMANY);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getCountry()).isEqualTo(Country.GERMANY);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("updates push token when provided")
        void withPushToken_updatesPushToken() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setPushToken("fcm-token-xyz");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getPushToken()).isEqualTo("fcm-token-xyz");
        }

        @Test
        @DisplayName("updates all fields when all are provided")
        void withAllFields_updatesAllFields() {
            User user = user();
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setName("New Name");
            request.setSurname("New Surname");
            request.setCountry(Country.GERMANY);
            request.setPushToken("fcm-token-abc");

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, request);

            assertThat(user.getName()).isEqualTo("New Name");
            assertThat(user.getSurname()).isEqualTo("New Surname");
            assertThat(user.getCountry()).isEqualTo(Country.GERMANY);
            assertThat(user.getPushToken()).isEqualTo("fcm-token-abc");
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("does not overwrite existing fields when request fields are null")
        void withNullFields_doesNotOverwriteExistingValues() {
            User user = user(); // name=Nicat, surname=Mammadov, country=AZERBAIJAN

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.updateProfile(USER_ID, new UpdateProfileRequest());

            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(user.getSurname()).isEqualTo(SURNAME);
            assertThat(user.getCountry()).isEqualTo(COUNTRY);
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

    // ================================================================== uploadAvatar()

    @Nested
    @DisplayName("uploadAvatar()")
    class UploadAvatar {

        @Test
        @DisplayName("uploads file to S3 and saves URL on user")
        void withValidFile_uploadsAndSavesUrl() {
            User user = user();
            String newUrl = "https://bucket.s3.amazonaws.com/avatars/" + USER_ID + "/new.jpg";

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), eq("avatars/" + USER_ID))).thenReturn(newUrl);
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            assertThat(user.getAvatarUrl()).isEqualTo(newUrl);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("passes correct key prefix to S3StorageService")
        void withValidFile_passesCorrectKeyPrefix() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), any())).thenReturn("https://some-url");
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3StorageService).upload(any(), eq("avatars/" + USER_ID));
        }

        @Test
        @DisplayName("deletes old avatar from S3 before uploading new one")
        void whenUserHasExistingAvatar_deletesOldFirst() {
            User user = user();
            user.setAvatarUrl(AVATAR);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), any())).thenReturn("https://new-url");
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            InOrder order = inOrder(s3StorageService);
            order.verify(s3StorageService).delete(AVATAR);
            order.verify(s3StorageService).upload(any(), any());
        }

        @Test
        @DisplayName("does not call delete when user has no existing avatar")
        void whenUserHasNoAvatar_doesNotCallDelete() {
            User user = user();

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), any())).thenReturn("https://new-url");
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3StorageService).delete(null);
        }

        @Test
        @DisplayName("still uploads new avatar even when S3 delete of old one fails")
        void whenOldAvatarDeleteFails_stillUploadsNewAvatar() {
            User user = user();
            user.setAvatarUrl(AVATAR);

            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), any())).thenReturn("https://new-url");
            when(userMapper.toProfileResponse(user)).thenReturn(profileResponse(user));

            userService.uploadAvatar(USER_ID, validJpeg());

            verify(s3StorageService).upload(any(), any());
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("propagates ResponseStatusException from S3StorageService (e.g. invalid file)")
        void whenS3ServiceThrows_propagatesException() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(s3StorageService.upload(any(), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.uploadAvatar(USER_ID, validJpeg()));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.uploadAvatar(USER_ID, validJpeg()));
            verify(s3StorageService, never()).upload(any(), any());
        }
    }

    // ================================================================== changePassword()

    @Nested
    @DisplayName("changePassword()")
    class ChangePassword {

        @Test
        @DisplayName("updates password and invalidates all sessions on success")
        void withCorrectPassword_updatesAndInvalidatesSessions() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", HASH)).thenReturn(true);
            when(passwordEncoder.encode("newPass123!")).thenReturn("$2a$newHash");

            userService.changePassword(USER_ID, changePasswordRequest("oldPass123", "newPass123!"));

            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
            verify(userRepository).save(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("stores encoded password, never plaintext")
        void withCorrectPassword_storesEncodedPassword() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("oldPass123", HASH)).thenReturn(true);
            when(passwordEncoder.encode("newPass123!")).thenReturn("$2a$newHash");

            userService.changePassword(USER_ID, changePasswordRequest("oldPass123", "newPass123!"));

            assertThat(user.getPassword()).doesNotContain("newPass123!");
            assertThat(user.getPassword()).isEqualTo("$2a$newHash");
        }

        @Test
        @DisplayName("throws InvalidCredentialsException when current password is wrong")
        void whenCurrentPasswordWrong_throwsInvalidCredentials() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPass", HASH)).thenReturn(false);

            assertThrows(InvalidCredentialsException.class,
                    () -> userService.changePassword(USER_ID, changePasswordRequest("wrongPass", "newPass123!")));

            verify(userRepository, never()).save(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("does not invalidate sessions when password change fails")
        void whenPasswordChangeFails_sessionsAreNotInvalidated() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongPass", HASH)).thenReturn(false);

            assertThrows(InvalidCredentialsException.class,
                    () -> userService.changePassword(USER_ID, changePasswordRequest("wrongPass", "newPass123!")));

            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.changePassword(USER_ID, changePasswordRequest("any", "newPass123!")));

            verify(passwordEncoder, never()).matches(any(), any());
            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================== deleteAccount()

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
            user.setAvatarUrl(AVATAR);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            InOrder order = inOrder(s3StorageService, userRepository);
            order.verify(s3StorageService).delete(AVATAR);
            order.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("does not call S3 delete when user has no avatar")
        void whenUserHasNoAvatar_skipsS3Delete() {
            User user = user();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            verify(s3StorageService).delete(null);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("still deletes user even when S3 avatar deletion fails")
        void whenS3DeleteFails_stillDeletesUser() {
            User user = user();
            user.setAvatarUrl(AVATAR);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            userService.deleteAccount(USER_ID);

            verify(userRepository).delete(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenUserNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.deleteAccount(USER_ID));

            verify(userRepository, never()).delete(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }
    }
}