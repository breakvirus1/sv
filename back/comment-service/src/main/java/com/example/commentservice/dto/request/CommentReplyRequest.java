package com.example.commentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentReplyRequest {
    @NotNull(message = "parentCommentId обязателен")
    private Long parentCommentId;

    private Long parentReplyId;

    @NotBlank(message = "Тело ответа не может быть пустым")
    private String body;
}
