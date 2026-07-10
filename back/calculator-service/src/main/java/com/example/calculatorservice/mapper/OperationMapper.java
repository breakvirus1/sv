package com.example.calculatorservice.mapper;

import com.example.calculatorservice.dto.MaterialDto;
import com.example.calculatorservice.dto.OperationCreateRequest;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.dto.OperationUpdateRequest;
import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.Operation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperationMapper {

    @Mapping(target = "unit", expression = "java(operation.getUnit() != null ? operation.getUnit().getDisplayName() : null)")
    OperationDto toDto(Operation operation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Operation toEntity(OperationCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(OperationUpdateRequest request, @MappingTarget Operation operation);

    @Mapping(target = "pricePerSquareMeter", source = "pricePerSquareMeter")
    @Mapping(target = "type", ignore = true)
    MaterialDto toMaterialDto(Material material);

    @Named("determineMaterialType")
    default String determineMaterialType(Material material) {
        String name = material.getName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.contains("баннер")) {
                return "BANNER";
            } else if (lower.contains("плёнка") || lower.contains("пленка")) {
                return "PLENKA";
            }
        }
        return "UNKNOWN";
    }
}
