package com.example.common.config;

import org.mapstruct.factory.Mappers;
import org.mapstruct.ReportingPolicy;

@org.mapstruct.MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MapperConfig {
    MapperConfig INSTANCE = Mappers.getMapper(MapperConfig.class);
}
