package com.example.common.entity;

/**
 * Стадии производства — шаги, через которые проходит заказ.
 * Каждая стадия соответствует цеху (Workshop) или группе операций.
 * Пример последовательности: Дизайн → Печать → Постпечать → Контроль качества → Упаковка → Доставка.
 */
public enum ProductionStage {
    /** Еще не начато */
    NOT_STARTED("Не начат"),
    /** Этап проектирования макета */
    DESIGN("Дизайн"),
    /** Основная печать */
    PRINTING("Печать"),
    /** Постобработка: ламинация, резка, оклейка и т.д. */
    FINISHING("Постпечать"),
    /** Проверка качества готовой продукции */
    QUALITY_CONTROL("Контроль качества"),
    /** Упаковка заказа */
    PACKAGING("Упаковка"),
    /** Доставка клиенту или передача в отдел доставки */
    SHIPPING("Доставка");

    private final String displayName;

    ProductionStage(String displayName) {
        this.displayName = displayName;
    }

    /** Возвращает отображаемое имя стадии на русском */
    public String getDisplayName() {
        return displayName;
    }
}
