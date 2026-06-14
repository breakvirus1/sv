package com.example.materialservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * Динамический параметр операции.
 * Пример: "width" → "Ширина, мм", type: "NUMBER", unit: "мм"
 */
@Entity
@Table(name = "operation_parameters", schema = "svtables")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OperationParameter extends BaseEntity {

    /** Ссылка на операцию */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operation_id", nullable = false)
    private MaterialOperation operation;

    /** Ключ параметра (используется в JSON) */
    @Column(name = "param_key", nullable = false, length = 50)
    private String paramKey;

    /** Отображаемое название для формы */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** Тип поля: NUMBER, TEXT, SELECT, CHECKBOX */
    @Column(name = "type", nullable = false, length = 20)
    private String type = "NUMBER";

    /** Единица измерения (мм, м, "") */
    @Column(name = "unit", length = 20)
    private String unit = "";

    /** Значение по умолчанию */
    @Column(name = "default_value", length = 255)
    private String defaultValue = "";

    /** Обязательное поле? */
    @Column(name = "required")
    @ColumnDefault("false")
    private Boolean required = false;

    /** Порядок отображения в форме */
    @Column(name = "sort_order")
    @ColumnDefault("0")
    private Integer sortOrder = 0;

    /** Описание параметра (подсказка для пользователя) */
    @Column(name = "description", length = 500)
    private String description = "";
}
