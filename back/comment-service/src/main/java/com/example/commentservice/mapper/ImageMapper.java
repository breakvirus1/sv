package com.example.commentservice.mapper;

import com.example.commentservice.dto.response.ImageResponse;
import com.example.commentservice.entity.Image;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ImageMapper {
    @org.mapstruct.Mapping(target = "url", expression = "java(\"/api/v1/images/\" + image.getId())")
    ImageResponse toDto(Image image);
}
