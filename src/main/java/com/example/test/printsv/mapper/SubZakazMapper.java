package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubZakazMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "zakaz", ignore = true)
    @Mapping(target = "name", source = "name")
    SubZakaz toEntity(SubZakazRequest subZakazRequest);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "zakazId", source = "zakaz.id")
    SubZakazResponse toSubZakazResponse(SubZakaz subZakaz);

    @Mapping(target = "name", source = "name")
    SubZakazRequest toSubZakazRequest(SubZakaz subZakaz);
}