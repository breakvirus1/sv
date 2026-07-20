package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Сущность "Группировка операций" — слово, по которому группируются операции.
 * Пример: "печать", "ламинирование", "резка".
 */
@Entity
@Table(name = "operation_groups", schema = "svschema")
@Getter
@Setter
public class OperationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255, unique = true)
    private String name;
}
