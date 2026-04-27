package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность "Комментарий" — сообщение, связанное с заказом.
 * Может быть внешним (видимым клиенту) или внутренним (только для сотрудников).
 * Пример: "Просим ускорить печать", "Клиент согласен на доплату".
 */
@Entity
@Table(name = "order_comments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderComment extends BaseEntity {

    /** Заказ, к которому относится комментарий */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Автор комментария (сотрудник) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Employee author;

    /** Текст комментария */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** Флаг внутреннего комментария (не показывается клиенту) */
    @Column(name = "is_internal")
    private Boolean isInternal = false;
}
