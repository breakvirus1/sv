package com.example.test.printsv.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;

@Mapper(componentModel = "spring")
public interface SubZakazMapper {
    SubZakaz toSubZakaz(SubZakazRequest subZakazRequest);
    
@Mapping(source = "material.name", target = "materialName")
    SubZakazResponse toSubZakazResponse(SubZakaz subZakaz);
    

}
