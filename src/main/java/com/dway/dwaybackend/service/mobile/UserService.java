package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.auth.InvalidCredentialsException;
import com.dway.dwaybackend.common.exception.auth.UserNotFoundException;
import com.dway.dwaybackend.dto.request.user.ChangePasswordRequest;
import com.dway.dwaybackend.dto.request.user.UpdateProfileRequest;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.User;
import com.dway.dwaybackend.mapper.UserMapper;
import com.dway.dwaybackend.repository.RefreshTokenRepository;
import com.dway.dwaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3Client s3Client;
    private final UserMapper userMapper;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

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
        if (request.getCountry() != null)
        {
            user.setCountry(request.getCountry());
        }
        if (request.getPushToken() != null)
        {
            user.setPushToken(request.getPushToken());
        }

        userRepository.save(user);
        log.info("User {} updated profile", userId);
        return userMapper.toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        validateImageFile(file);

        User user = findUserById(userId);

        if (user.getAvatarUrl() != null) {
            deleteFromS3(user.getAvatarUrl());
        }

        String key = "avatars/" + userId + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromBytes(file.getBytes())
            );
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file");
        }

        String avatarUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        log.info("User {} uploaded avatar: {}", userId, key);
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

        if (user.getAvatarUrl() != null) {
            deleteFromS3(user.getAvatarUrl());
        }

        refreshTokenRepository.deleteAllByUserId(userId);
        userRepository.delete(user);

        log.info("User {} deleted their account", userId);
    }


    private User findUserById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }
        if (file.getSize() > 5L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avatar must not exceed 5 MB");
        }
    }

    private void deleteFromS3(String url) {
        try {
            // URL format: https://{bucket}.s3.{region}.amazonaws.com/{key}
            String prefix = "amazonaws.com/";
            int idx = url.indexOf(prefix);
            if (idx != -1) {
                String key = url.substring(idx + prefix.length());
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
            }
        } catch (Exception e) {
            // Non-fatal — log and continue so the new upload still proceeds
            log.warn("Failed to delete old avatar from S3: {}", url, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}