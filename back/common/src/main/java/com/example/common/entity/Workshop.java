package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Цех" (мастерская) — справочник производственных подразделений.
 * Примеры: "Печать", "Постпечать", "Люверсы".
 * Определяет последовательность этапов производства.
 */
@Entity
@Table(name = "workshops")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Workshop extends BaseEntity {

    /** Наименование цеха */
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    /** Порядок сортировки (чем меньше, тем раньше в последовательности) */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /** Этапы производства, выполняемые в этом цехе */
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStage> orderStages = new ArrayList<>();
}
