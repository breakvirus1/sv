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

    /** Абсолютный путь для сохранения файлов заказов компании. */
    @Size(max = 500, message = "Путь не может быть длиннее 500 символов")
    private String fileStoragePath;

    public CompanyCreateRequest(String name) {
        this.name = name;
    }
}
