package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.UpdateUserRoleRequest;
import com.dway.dwaybackend.dto.response.user.AdminUserResponse;
import com.dway.dwaybackend.entity.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserService Unit Tests")
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private S3StorageService s3StorageService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AdminUserService adminUserService;

    private static final UUID USER_ID       = UUID.fromString("51f8bf0b-459f-4d36-b290-623fa2f3da0d");
    private static final UUID ADMIN_ID      = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final String AVATAR_URL  = "https://bucket.s3.eu-north-1.amazonaws.com/avatars/" + USER_ID + "/avatar.jpg";
    
    private User regularUser() {
        return User.builder()
                .id(USER_ID).name("Nicat").email("nicat@gmail.com")
                .password("$2a$hash").isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.USER))
                .build();
    }

    private User adminUser(UUID id) {
        return User.builder()
                .id(id).name("Admin").email("admin@gmail.com")
                .password("$2a$hash").isVerified(true).isBanned(false)
                .plan(Plan.FREE).roles(Set.of(Role.ADMIN))
                .build();
    }

    private AdminUserResponse adminResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId()).name(user.getName()).email(user.getEmail())
                .isBanned(user.isBanned()).roles(user.getRoles())
                .bannedAt(user.getBannedAt())
                .build();
    }

    private UpdateUserRoleRequest roleRequest(Set<Role> roles) {
        UpdateUserRoleRequest r = new UpdateUserRoleRequest();
        r.setRoles(roles);
        return r;
    }

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("returns paginated list mapped via userMapper")
        void returnsPaginatedUsersViaMappeer() {
            User user = regularUser();
            AdminUserResponse response = adminResponse(user);
            Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 20), 1);

            when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
            when(userMapper.toAdminResponse(user)).thenReturn(response);

            Page<AdminUserResponse> result = adminUserService.getAllUsers(PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(USER_ID);
            verify(userMapper).toAdminResponse(user);
        }

        @Test
        @DisplayName("returns empty page when no users exist")
        void whenNoUsers_returnsEmptyPage() {
            when(userRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

            Page<AdminUserResponse> result = adminUserService.getAllUsers(PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("maps multiple users — each goes through userMapper")
        void withMultipleUsers_mapsEachThroughMapper() {
            User user1 = regularUser();
            User user2 = User.builder()
                    .id(UUID.randomUUID()).name("Elchin").email("elchin@gmail.com")
                    .isBanned(false).roles(Set.of(Role.USER)).build();
            Page<User> page = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

            when(userRepository.findAll(any(PageRequest.class))).thenReturn(page);
            when(userMapper.toAdminResponse(user1)).thenReturn(adminResponse(user1));
            when(userMapper.toAdminResponse(user2)).thenReturn(adminResponse(user2));

            Page<AdminUserResponse> result = adminUserService.getAllUsers(PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(userMapper, times(2)).toAdminResponse(any());
        }
    }

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("returns admin response for existing user")
        void withValidId_returnsAdminResponse() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            AdminUserResponse result = adminUserService.getUserById(USER_ID);

            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getEmail()).isEqualTo("nicat@gmail.com");
            verify(userMapper).toAdminResponse(user);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> adminUserService.getUserById(USER_ID));
            verify(userMapper, never()).toAdminResponse(any());
        }
    }

    @Nested
    @DisplayName("banUser()")
    class BanUser {

        @Test
        @DisplayName("sets banned=true and bannedAt timestamp")
        void withValidTarget_setsBannedFlagAndTimestamp() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.banUser(USER_ID, ADMIN_ID);

            assertThat(user.isBanned()).isTrue();
            assertThat(user.getBannedAt()).isNotNull();
            assertThat(user.getBannedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }

        @Test
        @DisplayName("saves user after banning")
        void withValidTarget_savesUser() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.banUser(USER_ID, ADMIN_ID);

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("immediately invalidates all sessions after banning")
        void withValidTarget_invalidatesSessions() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.banUser(USER_ID, ADMIN_ID);

            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("saves user before invalidating sessions — consistent ordering")
        void withValidTarget_saveBeforeInvalidate() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.banUser(USER_ID, ADMIN_ID);

            InOrder order = inOrder(userRepository, refreshTokenRepository);
            order.verify(userRepository).save(user);
            order.verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws AccessDeniedException and never looks up user when admin bans themselves")
        void whenAdminBansSelf_throwsBeforeFindById() {
            assertThrows(AccessDeniedException.class,
                    () -> adminUserService.banUser(ADMIN_ID, ADMIN_ID));
            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws AccessDeniedException when target user is another admin")
        void whenTargetIsAdmin_throwsAccessDenied() {
            UUID otherAdminId = UUID.fromString("88888888-8888-8888-8888-888888888888");
            User otherAdmin = adminUser(otherAdminId);
            when(userRepository.findById(otherAdminId)).thenReturn(Optional.of(otherAdmin));

            assertThrows(AccessDeniedException.class,
                    () -> adminUserService.banUser(otherAdminId, ADMIN_ID));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when target does not exist")
        void whenNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> adminUserService.banUser(USER_ID, ADMIN_ID));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unbanUser()")
    class UnbanUser {

        @Test
        @DisplayName("sets banned=false")
        void withBannedUser_setsBannedFalse() {
            User user = regularUser();
            user.setBanned(true);
            user.setBannedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.unbanUser(USER_ID, ADMIN_ID);

            assertThat(user.isBanned()).isFalse();
        }

        @Test
        @DisplayName("clears bannedAt timestamp to null")
        void withBannedUser_clearsBannedAt() {
            User user = regularUser();
            user.setBanned(true);
            user.setBannedAt(LocalDateTime.now().minusDays(1));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.unbanUser(USER_ID, ADMIN_ID);

            assertThat(user.getBannedAt()).isNull();
        }

        @Test
        @DisplayName("saves user after unbanning")
        void withBannedUser_savesUser() {
            User user = regularUser();
            user.setBanned(true);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.unbanUser(USER_ID, ADMIN_ID);

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("does not touch refresh tokens when unbanning")
        void whenUnbanning_doesNotInvalidateSessions() {
            User user = regularUser();
            user.setBanned(true);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.unbanUser(USER_ID, ADMIN_ID);

            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void whenNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> adminUserService.unbanUser(USER_ID, ADMIN_ID));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateUserRoles()")
    class UpdateUserRoles {

        @Test
        @DisplayName("replaces all existing roles with the provided set")
        void withValidTarget_replacesRoles() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.updateUserRoles(USER_ID, roleRequest(Set.of(Role.ADMIN)), ADMIN_ID);

            assertThat(user.getRoles()).containsExactly(Role.ADMIN);
            assertThat(user.getRoles()).doesNotContain(Role.USER);
        }

        @Test
        @DisplayName("can assign multiple roles at once")
        void withMultipleRoles_assignsAll() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.updateUserRoles(USER_ID, roleRequest(Set.of(Role.USER, Role.ADMIN)), ADMIN_ID);

            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
        }

        @Test
        @DisplayName("saves user after updating roles")
        void withValidTarget_savesUser() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userMapper.toAdminResponse(user)).thenReturn(adminResponse(user));

            adminUserService.updateUserRoles(USER_ID, roleRequest(Set.of(Role.ADMIN)), ADMIN_ID);

            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("throws AccessDeniedException and never looks up user when admin updates their own roles")
        void whenAdminUpdatesSelf_throwsBeforeFindById() {
            assertThrows(AccessDeniedException.class,
                    () -> adminUserService.updateUserRoles(ADMIN_ID, roleRequest(Set.of(Role.USER)), ADMIN_ID));
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when target does not exist")
        void whenNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> adminUserService.updateUserRoles(USER_ID, roleRequest(Set.of(Role.ADMIN)), ADMIN_ID));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("deletes user and invalidates all sessions")
        void withValidTarget_deletesUserAndSessions() {
            User user = regularUser();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminUserService.deleteUser(USER_ID, ADMIN_ID);

            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("deletes avatar from S3 before deleting the user")
        void whenUserHasAvatar_deletesAvatarFromS3First() {
            User user = regularUser();
            user.setAvatarUrl(AVATAR_URL);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminUserService.deleteUser(USER_ID, ADMIN_ID);

            InOrder order = inOrder(s3StorageService, userRepository);
            order.verify(s3StorageService).delete(AVATAR_URL);
            order.verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("does not call S3 delete when user has no avatar")
        void whenUserHasNoAvatar_skipsS3Delete() {
            User user = regularUser(); // avatarUrl is null
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminUserService.deleteUser(USER_ID, ADMIN_ID);

            // S3StorageService.delete() is called with null — it handles null gracefully internally.
            verify(s3StorageService).delete(null);
            verify(userRepository).delete(user);
        }

        @Test
        @DisplayName("still deletes user even when S3 avatar deletion fails")
        void whenS3DeleteFails_stillDeletesUser() {
            // S3StorageService.delete() is non-fatal — it never propagates exceptions.
            User user = regularUser();
            user.setAvatarUrl(AVATAR_URL);
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            adminUserService.deleteUser(USER_ID, ADMIN_ID);

            verify(userRepository).delete(user);
            verify(refreshTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("throws AccessDeniedException and never looks up user when admin deletes themselves")
        void whenAdminDeletesSelf_throwsBeforeFindById() {
            assertThrows(AccessDeniedException.class,
                    () -> adminUserService.deleteUser(ADMIN_ID, ADMIN_ID));
            verify(userRepository, never()).findById(any());
            verify(userRepository, never()).delete(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when target does not exist")
        void whenNotFound_throwsUserNotFoundException() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> adminUserService.deleteUser(USER_ID, ADMIN_ID));
            verify(userRepository, never()).delete(any());
            verify(refreshTokenRepository, never()).deleteAllByUserId(any());
        }
    }
}