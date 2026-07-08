package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Getter
@Setter
@Entity
@Table(name = "calculator_material_operations", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.calculator_material_operations SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
public class MaterialOperation extends BaseEntity {

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;
}
