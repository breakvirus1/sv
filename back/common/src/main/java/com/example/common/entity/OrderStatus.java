package com.example.common.entity;

/**
 * Статусы заказов в производственной линии.
 * Жизненный цикл заказа проходит через эти статусы.
 */
public enum OrderStatus {
    /** Заказ создан, ожидает подтверждения/старта */
    WAITING("Ожидание"),
    /** Заказ запущен в производство */
    LAUNCHED("Запущен"),
    /** Заказ находится в активном производстве */
    IN_PROGRESS("В работе"),
    /** Заказ полностью готов (выполнены все этапы) */
    READY("Готов"),
    /** Заказ принят клиентом */
    ACCEPTED("Принят"),
    /** Заказ закрыт (финансово и логистически завершен) */
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
