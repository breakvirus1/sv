package com.example.orderservice.product.mapper;

import com.example.orderservice.product.Product;
import com.example.orderservice.product.ProductMaterial;
import com.example.orderservice.product.ProductOperation;
import com.example.orderservice.product.dto.ProductDTO;
import com.example.orderservice.product.dto.ProductMaterialDTO;
import com.example.orderservice.product.dto.ProductOperationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Duration;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductMapper INSTANCE = Mappers.getMapper(ProductMapper.class);

    ProductDTO toDto(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "operations", ignore = true)
    Product toEntity(ProductDTO dto);

    @Mapping(target = "materialName", source = "material.name")
    @Mapping(target = "price", source = "material.price")
    @Mapping(target = "unit", source = "material.unit")
    ProductMaterialDTO toMaterialDto(ProductMaterial entity);

    @Mapping(target = "normTime", expression = "java(mapDuration(entity.getNormTime()))")
    ProductOperationDTO toOperationDto(ProductOperation entity);

    default String mapDuration(Duration duration) {
        return duration != null ? duration.toString() : null;
    }
}
