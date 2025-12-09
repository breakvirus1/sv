package com.example.test.printsv.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.test.printsv.entity.Material;
import com.example.test.printsv.entity.Zakaz;

import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;

@Mapper(componentModel = "spring")
public interface ZakazMapper {
    
    
    ZakazResponse toZakazResponse(Zakaz zakaz);
    
    Zakaz toZakaz(ZakazRequest request);    
    
    
}
