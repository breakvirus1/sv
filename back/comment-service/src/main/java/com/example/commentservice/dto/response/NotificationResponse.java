package com.example.commentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String message;
    private String type;
    private Long referenceId;
    private String referenceType;
    private Boolean readed;
    private LocalDateTime createdAt;
}
