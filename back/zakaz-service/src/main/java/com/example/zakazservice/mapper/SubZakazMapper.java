package com.example.zakazservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.common.dto.SubZakazDto;
import com.example.common.entity.SubZakaz;
import com.example.zakazservice.response.SubZakazResponse;

@Mapper(componentModel = "spring")
public interface SubZakazMapper {
    SubZakazDto toSubZakazDto(SubZakaz entity);
    SubZakaz toEntity(SubZakazDto dto);

    @Mapping(source = "material.name", target = "materialName")
    SubZakazResponse toSubZakazResponse(SubZakaz entity);
}
