package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.time.LocalDate;

@Entity
@Table(name = "order_stages")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "OrderStage", usage = CacheConcurrencyStrategy.READ_WRITE)
public class OrderStage extends BaseEntity {

    /** Заказ, для которого предназначен этап */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Цех, выполняющий этот этап */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id", nullable = false)
    private Workshop workshop;

    /** Ждать завершения всех предыдущих цехов перед началом этого? */
    @Column(name = "wait_previous")
    private Boolean waitPrevious = true;

    /** Планируемый срок выполнения этапа */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Примечание к этапу (технические указания и т.п.) */
    @Column(length = 500)
    private String note;

    /** Статус этапа: TODO (не начат), IN_PROGRESS (в работе), DONE (завершен) */
    @Column(length = 50)
    private String status = "TODO";

    /** Исходные файлы для этого этапа (пути к файлам или JSON с метаданными) */
    @Column(name = "source_files", columnDefinition = "TEXT")
    private String sourceFiles;
}
