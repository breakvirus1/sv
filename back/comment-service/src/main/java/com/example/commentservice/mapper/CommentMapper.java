package com.example.commentservice.mapper;

import com.example.commentservice.dto.request.CommentRequest;
import com.example.commentservice.dto.response.CommentResponse;
import com.example.commentservice.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CommentReplyMapper.class, ImageMapper.class})
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "images", ignore = true)
    Comment toEntity(CommentRequest request);

    CommentResponse toDto(Comment comment);
}
