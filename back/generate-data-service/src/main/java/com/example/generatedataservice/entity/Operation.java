package com.example.generatedataservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "calculator_operations", schema = "calculator")
@Getter @Setter
public class Operation extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", length = 20)
    private UnitType unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_to", length = 20)
    private ApplicableType applicableTo;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "hem_width_mm")
    private Integer hemWidthMm;

    @Column(name = "hem_count")
    private Integer hemCount;
}
