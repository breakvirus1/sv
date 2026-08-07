package com.example.commentservice.mapper;

import com.example.commentservice.dto.request.CommentReplyRequest;
import com.example.commentservice.dto.response.CommentReplyResponse;
import com.example.commentservice.entity.CommentReply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentReplyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "parentReply", ignore = true)
    @Mapping(target = "replies", ignore = true)
    CommentReply toEntity(CommentReplyRequest request);

    CommentReplyResponse toDto(CommentReply commentReply);
}
