package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.challenge.CreateChallengeRequest;
import com.dway.dwaybackend.dto.response.challenge.AdminChallengeResponse;
import com.dway.dwaybackend.dto.response.challenge.ChallengeResponse;
import com.dway.dwaybackend.entity.Challenge;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChallengeMapper {

    @Mapping(target = "isJoined", ignore = true)
    @Mapping(target = "progress", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    ChallengeResponse toResponse(Challenge challenge);

    @Mapping(source = "active", target = "isActive")
    AdminChallengeResponse toAdminResponse(Challenge challenge);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "participantCount", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Challenge toEntity(CreateChallengeRequest request);
}