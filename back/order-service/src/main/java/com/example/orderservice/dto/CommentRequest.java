package com.example.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO для добавления комментария к заказу.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequest {
    /** Текст комментария */
    private String message;
    /** Флаг внутреннего комментария (не показывать клиенту) */
    private Boolean isInternal;
}
