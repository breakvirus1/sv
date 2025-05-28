package com.example.test.printsv.mapper;

import org.mapstruct.Mapper;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;


@Mapper(componentModel = "spring")
public interface SubZakazMapper {
    SubZakaz toEntity(SubZakazRequest subZakazRequest);
    SubZakazRequest toSubzakazRequest(SubZakaz subZakaz);
    SubZakazResponse toSubzakazResponse(SubZakaz subZakaz);

}
