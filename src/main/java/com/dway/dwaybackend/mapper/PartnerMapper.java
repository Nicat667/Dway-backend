package com.dway.dwaybackend.mapper;

import com.dway.dwaybackend.dto.request.partner.CreatePartnerRequest;
import com.dway.dwaybackend.dto.response.partner.PartnerResponse;
import com.dway.dwaybackend.entity.Partner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PartnerMapper {

    // Partner.isActive() is a primitive boolean getter — MapStruct strips the "is" prefix
    // and sees the source property as "active", not "isActive". The target builder method
    // is named "isActive", so without an explicit mapping they never connect.
    @Mapping(source = "active", target = "isActive")
    PartnerResponse toResponse(Partner partner);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iconUrl", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Partner toEntity(CreatePartnerRequest request);
}