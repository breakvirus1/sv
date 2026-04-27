package com.example.common.entity;

/**
 * Тип клиента.
 * Частник — физическое лицо.
 * Компания — юридическое лицо (организация).
 */
public enum ClientType {
    /** Физическое лицо (частный заказчик) */
    PRIVATE("Частник"),
    /** Юридическое лицо (компания, организация) */
    COMPANY("Компания");

    private final String displayName;

    ClientType(String displayName) {
        this.displayName = displayName;
    }

    /** Возвращает отображаемое имя (например, "Частник") */
    public String getDisplayName() {
        return displayName;
    }
}
