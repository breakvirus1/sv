package com.example.clientservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyCreateRequest {
    @NotBlank(message = "Название компании обязательно")
    @Size(max = 255, message = "Название компании не может быть длиннее 255 символов")
    private String name;
}
