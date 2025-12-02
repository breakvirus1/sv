package com.example.test.printsv.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.test.printsv.entity.Material;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.request.ListZakazByUserIdRequest;
import com.example.test.printsv.response.ListZakazByUserIdResponse;
import com.example.test.printsv.response.ZakazResponse;

@Mapper(componentModel = "spring")
public interface ZakazMapper {
    
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "zakazList", target = "zakazList")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "sum", target = "sum")
    @Mapping(source = "id", target = "id")
    ListZakazByUserIdResponse toListZakazByUserIdResponse(Zakaz zakaz);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "sum", target = "sum")
    @Mapping(source = "id", target = "id")
    ZakazResponse toZakazResponse(Zakaz zakaz);
    @Mapping(source = "userId", target = "user.id")
     Zakaz toZakaz(ListZakazByUserIdRequest request);

}
