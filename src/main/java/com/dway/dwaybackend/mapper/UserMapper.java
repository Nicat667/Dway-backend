package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.response.user.AdminUserResponse;
import com.dway.dwaybackend.dto.response.user.UserProfileResponse;
import com.dway.dwaybackend.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toProfileResponse(User user);

    AdminUserResponse toAdminResponse(User user);
}