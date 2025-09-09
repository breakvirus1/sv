package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubZakazMapper {

    @Mapping(source = "zakaz.id", target = "zakazId")
    SubZakazResponse toSubZakazResponse(SubZakaz subZakaz);

     @Mapping(target = "id", ignore = true)
     @Mapping(target = "zakaz", ignore = true)
     @Mapping(target = "material", ignore = true)
    SubZakaz toSubZakaz(SubZakazRequest subZakazRequest);
}
