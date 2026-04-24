package com.example.zakazservice.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.example.common.dto.ZakazDto;
import com.example.common.entity.SubZakaz;
import com.example.common.entity.Zakaz;
import com.example.zakazservice.response.ZakazResponse;

@Mapper(componentModel = "spring")
public interface ZakazMapper {
    ZakazDto toZakazDto(Zakaz entity);
    Zakaz toEntity(ZakazDto dto);

    @AfterMapping
    default void setSum(Zakaz zakaz, @MappingTarget ZakazResponse response) {
        if (zakaz.getSubZakazList() != null) {
            Double total = zakaz.getSubZakazList().stream()
                .mapToDouble(SubZakaz::getPrice)
                .sum();
            response.setSum(total);
        }
    }

    ZakazResponse toZakazResponse(Zakaz zakaz);
}
