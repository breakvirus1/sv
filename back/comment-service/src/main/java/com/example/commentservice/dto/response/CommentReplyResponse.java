package com.example.commentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentReplyResponse {
    private Long id;
    private Long orderId;
    private Long employeeId;
    private String employeeName;
    private Boolean readed;
    private String body;
    private Long parentCommentId;
    private Long parentReplyId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ImageResponse> images;
    private List<CommentReplyResponse> replies;
}
