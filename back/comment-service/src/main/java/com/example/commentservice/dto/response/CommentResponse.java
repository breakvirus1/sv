package com.example.commentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    private Long id;
    private Long orderId;
    private Long employeeId;
    private String employeeName;
    private Boolean readed;
    private String body;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentReplyResponse> replies;
}
