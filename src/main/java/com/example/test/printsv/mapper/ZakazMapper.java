package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {SubZakazMapper.class})
public interface ZakazMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subZakazList", ignore = true)
    @Mapping(target = "userOfZakaz", ignore = true)
    @Mapping(target = "customerOfZakaz", ignore = true)
    Zakaz toEntity(ZakazRequest zakazRequest);

    @Mapping(target = "customerId", source = "customerOfZakaz.id")
    @Mapping(target = "sum", ignore = true)
    ZakazRequest toZakazRequest(Zakaz zakaz);

    @Mapping(target = "userId", source = "userOfZakaz.id")
    @Mapping(target = "customerId", source = "customerOfZakaz.id")
    @Mapping(target = "subZakaz", source = "subZakazList")
    ZakazResponse fromZakazEntitytoZakazResponse(Zakaz zakaz);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "subZakaz", ignore = true)
    ZakazResponse fromZakazRequestToZakazResponse(ZakazRequest zakazRequest);
}