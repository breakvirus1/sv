package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;

@Entity
@Table(name = "material_operation_groups", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.material_operation_groups SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class MaterialOperationGroup extends BaseEntity {

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "operation_group_id", nullable = false)
    private Long operationGroupId;

    @Column(name = "operation_id", nullable = false)
    private Long operationId;

    @PrePersist
    protected void onCreate() {
        if (getCreatedAt() == null) setCreatedAt(LocalDateTime.now());
        if (getUpdatedAt() == null) setUpdatedAt(LocalDateTime.now());
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(LocalDateTime.now());
    }
}
