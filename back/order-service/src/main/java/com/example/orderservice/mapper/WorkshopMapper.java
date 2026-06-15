package com.example.orderservice.mapper;

import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Workshop;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkshopMapper {

    WorkshopResponse toDto(Workshop workshop);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "orderStages", ignore = true)
    Workshop toEntity(WorkshopCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "orderStages", ignore = true)
    void updateEntityFromRequest(WorkshopUpdateRequest request, @MappingTarget Workshop workshop);
}
