package com.example.calculatorservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

import com.example.calculatorservice.entity.UnitType;

@Data
public class OperationUpdateRequest {
    @NotBlank(message = "Название операции обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
    private BigDecimal price;

    @NotNull(message = "Единица измерения обязательна")
    private UnitType unit;

    private boolean isDefault;

    @DecimalMin(value = "0", message = "Ширина подгиба не может быть отрицательной")
    private Integer hemWidthMm;

    @DecimalMin(value = "0", message = "Количество подгибов не может быть отрицательным")
    private Integer hemCount;
}
