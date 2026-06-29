package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;

/**
 * Сущность "Люверс" — металлическое кольцо для крепления баннера.
 * Хранит информацию о диаметре и цене за штуку.
 */
@Entity
@Table(name = "calculator_eyelets", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.calculator_eyelets SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class Eyelet extends BaseEntity {

    /** Наименование (например, "Люверс 8 мм") */
    @Column(nullable = false, length = 255)
    private String name;

    /** Цена за 1 штуку */
    @Column(name = "price_per_piece", precision = 12, scale = 2)
    private BigDecimal pricePerPiece;

    /** Диаметр люверса в миллиметрах */
    @Column(name = "diameter_mm")
    private Integer diameterMm;
}
