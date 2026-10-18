package com.example.calculatorservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OperationGroupCreateRequest {
    @NotBlank(message = "Название группы обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;
}
