package com.example.calculatorservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Расчёт" — результат расчета себестоимости изделия.
 * Содержит параметры заказа (размеры, DPI, подворот, люверсы) и итоговую цену.
 */
@Entity
@Table(name = "calculator_calculations", schema = "svschema")
@SQLDelete(sql = "UPDATE svschema.calculator_calculations SET deleted = true WHERE id=?")
@Where(clause = "deleted = false")
@Getter
@Setter
public class Calculation extends BaseEntity {

    /** Материал (баннер или плёнка) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

     /** Ширина в метрах */
    @Column(name = "width_m", nullable = false, precision = 10, scale = 4)
    private BigDecimal widthM;

    /** Высота в метрах */
    @Column(name = "height_m", nullable = false, precision = 10, scale = 4)
    private BigDecimal heightM;

    /** Разрешение печати (DPI) */
    private Integer dpi;

    /** Припуск по горизонтали в миллиметрах (на каждую сторону) */
    @Column(name = "podvorot_mm_horizontal", precision = 10, scale = 2)
    private BigDecimal podvorotMmHorizontal;

    /** Припуск по вертикали в миллиметрах (на каждую сторону) */
    @Column(name = "podvorot_mm_vertical", precision = 10, scale = 2)
    private BigDecimal podvorotMmVertical;

    /** Количество подворотов на сторону (по умолчанию 2) */
    @Column(name = "podvorot_count_per_side")
    private Integer podvorotCountPerSide = 2;

    /** Выбранный люверс (если требуется) */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "eyelet_id")
    private Eyelet eyelet;

    /** Шаг установки люверсов в сантиметрах (по умолчанию 40 см) */
    @Column(name = "eyelet_step_cm")
    private Integer eyeletStepCm = 40;

    /** Итоговая себестоимость */
    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /** Выбранные операции с рассчитанными количествами и ценами */
    @OneToMany(mappedBy = "calculation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CalculationOperation> selectedOperations = new ArrayList<>();

    // ==================== Удобные методы ====================

    public void addOperation(CalculationOperation operation) {
        selectedOperations.add(operation);
        operation.setCalculation(this);
    }

    public boolean hasPodvorot() {
        return (podvorotMmHorizontal != null && podvorotMmHorizontal.compareTo(BigDecimal.ZERO) > 0) ||
               (podvorotMmVertical != null && podvorotMmVertical.compareTo(BigDecimal.ZERO) > 0);
    }

    public BigDecimal getArea() {
        return widthM.multiply(heightM);
    }
}
