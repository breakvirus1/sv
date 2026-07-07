package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Сущность "Группировка операций" — слово, по которому группируются операции.
 * Пример: "печать", "ламинирование", "резка".
 */
@Entity
@Table(name = "operation_groups", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.operation_groups SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class OperationGroup extends BaseEntity {

    @Column(nullable = false, length = 255, unique = true)
    private String name;
}
