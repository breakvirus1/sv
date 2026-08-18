package com.example.commentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImageResponse {
    private Long id;
    private String filename;
    private String originalName;
    private String contentType;
    private Long size;
    private String url;
    private LocalDateTime createdAt;
}
