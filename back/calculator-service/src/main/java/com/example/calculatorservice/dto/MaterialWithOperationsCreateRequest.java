package com.example.calculatorservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialWithOperationsCreateRequest {
    @NotBlank(message = "Название материала обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;

    @Size(max = 20, message = "Единица измерения не может быть длиннее 20 символов")
    private String unit;

    @NotNull(message = "Цена обязательна")
    @DecimalMin(value = "0", message = "Цена не может быть отрицательной")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "Коэффициент отхода не может быть отрицательным")
    private BigDecimal wasteCoefficient;

    private List<Long> operationIds;
}
