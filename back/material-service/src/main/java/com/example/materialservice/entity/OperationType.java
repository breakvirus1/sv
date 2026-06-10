package com.example.materialservice.entity;

/**
 * Типы операций над материалами
 */
public enum OperationType {
    PRINT,              // Печать
    LAMINATION,         // Ламинация / покрытие
    CUTTING,            // Раскрой
    WELDING,            // Сварка
    EYELETS,            // Люверсы
    POCKET,             // Карман под трубу
    INSTALLATION,       // Монтаж / оклейка
    ADDITIONAL_MATERIAL, // Дополнительные материалы (клей, скотч и т.д.)
    CUSTOM              // Кастомная операция
}
