package com.example.clientservice.mapper;

import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.entity.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompanyMapper {

    CompanyResponse toDto(Company company);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Company toEntity(CompanyCreateRequest request);

    default Company createFromRequest(CompanyCreateRequest request) {
        Company company = toEntity(request);
        return company;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CompanyCreateRequest request, @MappingTarget Company company);

    default void updateFromRequest(CompanyCreateRequest request, @MappingTarget Company company) {
        updateEntityFromRequest(request, company);
    }
}
