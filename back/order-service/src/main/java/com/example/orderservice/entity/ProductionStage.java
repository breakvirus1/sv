package com.example.orderservice.entity;

/**
 * Стадии производства — шаги, через которые проходит заказ.
 * Каждая стадия соответствует цеху (Workshop) или группе операций.
 * Пример последовательности: Дизайн → Печать → Постпечать → Контроль качества → Упаковка → Доставка.
 */
public enum ProductionStage {
    /** Черновик заказа */
    DRAFT("Черновик"),
    /** В работе */
    IN_PROGRESS("В работе"),
    /** Заказ готов к передаче/доставке */
    READY("Готов"),
    /** Заказ закрыт */
    CLOSED("Закрыт");

    private final String displayName;

    ProductionStage(String displayName) {
        this.displayName = displayName;
    }

    /** Возвращает отображаемое имя стадии на русском */
    public String getDisplayName() {
        return displayName;
    }
}
