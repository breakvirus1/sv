package com.example.orderservice.dto;

import com.example.employeeservice.dto.EmployeeResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO для комментария или сообщения к заказу.
 * Может быть внешним (для клиента) или внутренним (для сотрудников).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    /** ID комментария */
    private Long id;
    /** Текст комментария */
    private String message;
    /** Автор комментария (сотрудник) */
    private EmployeeResponse author;
    /** Флаг внутреннего комментария (не показывать клиенту) */
    private Boolean isInternal;
    /** Дата и время создания */
    private LocalDateTime timestamp;
}
