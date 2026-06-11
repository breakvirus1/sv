package com.example.materialservice.mapper;

import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.entity.Material;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MaterialMapper {

    MaterialResponse toDto(Material material);

    Material toEntity(MaterialCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(MaterialUpdateRequest request, @MappingTarget Material material);
}
