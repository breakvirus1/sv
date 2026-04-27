package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Сущность "Этап производства" — связь заказа с цехом.
 * Определяет, в каком цехе и в какой срок должен быть выполнен заказ.
 * Пример: Печать (цех "Печать") → срок 2024-12-01, ждать предыдущие = true.
 */
@Entity
@Table(name = "order_stages")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
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
