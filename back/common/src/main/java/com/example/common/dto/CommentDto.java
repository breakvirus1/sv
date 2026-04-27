package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для комментария или сообщения к заказу.
 * Может быть внешним (для клиента) или внутренним (для сотрудников).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    /** ID комментария */
    private Long id;
    /** Текст комментария */
    private String message;
    /** Автор комментария (сотрудник) */
    private EmployeeDto author;
    /** Флаг внутреннего комментария (не показывать клиенту) */
    private Boolean isInternal;
    /** Дата и время создания */
    private LocalDateTime timestamp;
}
