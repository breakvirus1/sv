package com.example.orderservice.entity;

import com.example.employeeservice.entity.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "order_comments")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "OrderComment", usage = CacheConcurrencyStrategy.READ_WRITE)
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
