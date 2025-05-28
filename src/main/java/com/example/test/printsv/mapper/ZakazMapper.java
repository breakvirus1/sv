package com.example.test.printsv.mapper;

import org.mapstruct.Mapper;

import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;

@Mapper
public interface ZakazMapper {

    
    Zakaz toEntity(ZakazRequest zakazRequest);
    ZakazRequest toZakazRequest(Zakaz zakaz);
    ZakazResponse toZakazResponse(Zakaz zakaz);
    

}
