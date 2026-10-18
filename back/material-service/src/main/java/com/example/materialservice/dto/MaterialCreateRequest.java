package com.example.materialservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO для создания нового материала.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialCreateRequest {
    /** Наименование материала или операции */
    @NotBlank(message = "Название материала обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;
    /** Единица измерения (м2, шт, п.м. и т.д.) */
    @NotBlank(message = "Единица измерения обязательна")
    @Size(max = 20, message = "Единица измерения не может быть длиннее 20 символов")
    private String unit;
    /** Цена за единицу */
    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
    private BigDecimal price;
    /** Коэффициент отхода */
    @DecimalMin(value = "0", message = "Коэффициент отхода не может быть отрицательным")
    private BigDecimal wasteCoefficient;
    /** Ширина по умолчанию в метрах */
    @DecimalMin(value = "0", inclusive = false, message = "Ширина должна быть больше нуля")
    private BigDecimal defaultWidthM;
    /** Высота по умолчанию в метрах */
    @DecimalMin(value = "0", inclusive = false, message = "Высота должна быть больше нуля")
    private BigDecimal defaultHeightM;
}
