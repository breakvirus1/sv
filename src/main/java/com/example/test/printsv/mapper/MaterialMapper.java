package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.Material;
import com.example.test.printsv.request.MaterialRequest;
import com.example.test.printsv.response.MaterialResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    MaterialResponse toMaterialResponse(Material material);


    @Mapping(source = "name", target = "name")
    Material toMaterial(MaterialRequest materialRequest);
}