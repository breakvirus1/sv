package com.example.orderservice.entity;

/**
 * Статусы заказов в производственной линии.
 * Жизненный цикл заказа проходит через эти статусы.
 */
public enum OrderStatus {
    /** Черновик заказа */
    DRAFT("Черновик"),
    /** На согласовании */
    APPROVAL("Согласование"),
    /** В работе */
    IN_PROGRESS("В работе"),
    /** Заказ готов */
    READY("Готов"),
    /** Заказ закрыт */
    CLOSED("Закрыт");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    /** Возвращает отображаемое имя статуса на русском */
    public String getDisplayName() {
        return displayName;
    }
}
