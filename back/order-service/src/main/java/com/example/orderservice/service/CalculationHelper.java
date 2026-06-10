package com.example.orderservice.service;

import java.math.BigDecimal;

/**
 * Вспомогательный класс с функциями для расчётов в формулах.
 * Используется в JEXL-контексте как объект "helper".
 */
public class CalculationHelper {

    /** Ширина изделия в метрах (из контекста расчета) */
    private final BigDecimal width;

    /** Высота изделия в метрах (из контекста расчета) */
    private final BigDecimal height;

    /**
     * Конструктор с размерами изделия.
     * @param width ширина в метрах
     * @param height высота в метрах
     */
    public CalculationHelper(BigDecimal width, BigDecimal height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Расчёт количества люверсов (сооружений крепления) для баннера.
     * Ширина и высота берутся из полей позиции заказа.
     *
     * @param step          шаг между люверсами в миллиметрах (может быть null, тогда 500)
     * @param edgeDistance  отступ от края в миллиметрах (может быть null, тогда 50)
     * @param orderQuantity количество изделий в заказе (может быть null, тогда 1)
     * @return общее количество люверсов (с учётом всех изделий)
     */
    public BigDecimal eyeletCount(Number step, Number edgeDistance, Number orderQuantity) {
        // Преобразуем размеры в миллиметры
        int wmm = width.multiply(BigDecimal.valueOf(1000)).intValue();
        int hmm = height.multiply(BigDecimal.valueOf(1000)).intValue();

        int s = (step != null) ? step.intValue() : 500;
        if (s <= 0) s = 500;

        int e = (edgeDistance != null) ? edgeDistance.intValue() : 50;
        if (e < 0) e = 0;

        // Эффективная длина размещения вдоль каждой стороны (без учёта отступа)
        int effW = wmm - 2 * e;
        int effH = hmm - 2 * e;
        if (effW < 0) effW = 0;
        if (effH < 0) effH = 0;

        // Количество промежутков (ceil division) для каждой стороны
        int ceilW = (effW == 0) ? 0 : (effW + s - 1) / s;
        int ceilH = (effH == 0) ? 0 : (effH + s - 1) / s;

        // Общее количество люверсов: 2 по ширине + 2 по высоте, минимум 4
        int count = 2 * ceilW + 2 * ceilH;
        if (count < 4) {
            count = 4;
        }

        int qty = (orderQuantity != null) ? orderQuantity.intValue() : 1;
        long total = (long) count * qty;
        return BigDecimal.valueOf(total);
    }
}