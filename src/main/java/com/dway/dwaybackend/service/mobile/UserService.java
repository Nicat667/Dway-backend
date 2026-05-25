package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.InvalidCredentialsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.ChangePasswordRequest;
import com.dway.dwaybackend.dto.request.user.UpdateProfileRequest;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.infrastructure.storage.S3StorageService;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3StorageService s3StorageService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(UUID userId) {
        return userMapper.toProfileResponse(findUserById(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getPushToken() != null) {
            user.setPushToken(request.getPushToken());
        }

        userRepository.save(user);
        log.info("User {} updated profile", userId);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        User user = findUserById(userId);

        s3StorageService.delete(user.getAvatarUrl());

        String avatarUrl = s3StorageService.upload(file, "avatars/" + userId);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("User {} uploaded avatar", userId);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all sessions — force re-login on all devices after password change
        refreshTokenRepository.deleteAllByUserId(userId);

        log.info("User {} changed password; all sessions invalidated", userId);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        User user = findUserById(userId);

        s3StorageService.delete(user.getAvatarUrl());
        refreshTokenRepository.deleteAllByUserId(userId);
        userRepository.delete(user);

        log.info("User {} deleted their account", userId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}