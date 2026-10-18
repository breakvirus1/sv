package com.example.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkshopCreateRequest {
    @NotBlank(message = "Название цеха обязательно")
    @Size(max = 255, message = "Название не может быть длиннее 255 символов")
    private String name;
    private Integer sortOrder;
    private List<Long> operationIds;
}
