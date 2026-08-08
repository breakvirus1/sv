package com.example.commentservice.mapper;

import com.example.commentservice.dto.response.NotificationResponse;
import com.example.commentservice.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationResponse toDto(Notification notification);
}
