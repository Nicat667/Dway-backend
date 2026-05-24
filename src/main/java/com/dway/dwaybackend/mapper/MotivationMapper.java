package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.motivation.CreateMotivationRequest;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.entity.DailyMotivation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MotivationMapper {

    MotivationResponse toResponse(DailyMotivation motivation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DailyMotivation toEntity(CreateMotivationRequest request);
}