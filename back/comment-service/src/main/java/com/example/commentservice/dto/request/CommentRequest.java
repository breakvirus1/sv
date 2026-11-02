package com.example.commentservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    @NotBlank(message = "Текст комментария обязателен")
    @Size(max = 2000, message = "Комментарий не может быть длиннее 2000 символов")
    private String body;
    private List<Long> imageIds;
}
