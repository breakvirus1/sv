package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.request.ZakazRequest;
//import com.example.test.printsv.response.ZakazListResponse;
import com.example.test.printsv.response.ZakazResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ZakazMapper {

//    @Mapping(source = "userOfZakaz_id", target = "userOfZakazId")
//    @Mapping(source = "customerOfZakaz_id", target = "customerOfZakazId")
//    @Mapping(source = "subZakazList", target = "subZakazList")
    ZakazResponse toZakazResponse(Zakaz zakaz);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subZakazList", ignore = true)
    @Mapping(target = "userOfZakaz", ignore = true)
    @Mapping(target = "customerOfZakaz", ignore = true)
    Zakaz toZakaz(ZakazRequest zakazRequest);





}