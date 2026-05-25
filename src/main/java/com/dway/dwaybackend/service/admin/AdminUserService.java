package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.AccessDeniedException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.UpdateUserRoleRequest;
import com.dway.dwaybackend.dto.response.user.AdminUserResponse;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.entity.enums.Role;
import com.dway.dwaybackend.infrastructure.storage.S3StorageService;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final S3StorageService s3StorageService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getAllUsers(Pageable pageable) {
        Pageable cleanPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return userRepository.findAll(cleanPageable).map(userMapper::toAdminResponse);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(UUID targetUserId) {
        return userMapper.toAdminResponse(findUserById(targetUserId));
    }

    @Transactional
    public AdminUserResponse banUser(UUID targetUserId, UUID adminId) {
        if (targetUserId.equals(adminId)) {
            throw new AccessDeniedException();
        }

        User user = findUserById(targetUserId);

        if (user.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException();
        }

        user.setBanned(true);
        user.setBannedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenRepository.deleteAllByUserId(targetUserId);

        log.info("Admin {} banned user {}", adminId, targetUserId);
        return userMapper.toAdminResponse(user);
    }

    @Transactional
    public AdminUserResponse unbanUser(UUID targetUserId, UUID adminId) {
        User user = findUserById(targetUserId);

        user.setBanned(false);
        user.setBannedAt(null);
        userRepository.save(user);

        log.info("Admin {} unbanned user {}", adminId, targetUserId);
        return userMapper.toAdminResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserRoles(UUID targetUserId, UpdateUserRoleRequest request, UUID adminId) {
        if (targetUserId.equals(adminId)) {
            throw new AccessDeniedException();
        }

        User user = findUserById(targetUserId);
        user.setRoles(request.getRoles());
        userRepository.save(user);

        log.info("Admin {} updated roles for user {} → {}", adminId, targetUserId, request.getRoles());
        return userMapper.toAdminResponse(user);
    }

    @Transactional
    public void deleteUser(UUID targetUserId, UUID adminId) {
        if (targetUserId.equals(adminId)) {
            throw new AccessDeniedException();
        }

        User user = findUserById(targetUserId);

        s3StorageService.delete(user.getAvatarUrl());
        refreshTokenRepository.deleteAllByUserId(targetUserId);
        userRepository.delete(user);

        log.info("Admin {} deleted user {}", adminId, targetUserId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}