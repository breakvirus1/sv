package com.example.employeeservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeCreateRequest {
    @NotBlank(message = "ФИО обязательно")
    @Size(max = 255, message = "ФИО не может быть длиннее 255 символов")
    private String fullName;

    @NotBlank(message = "Логин обязателен")
    @Size(max = 100, message = "Логин не может быть длиннее 100 символов")
    private String username;

    @Size(max = 100, message = "Должность не может быть длиннее 100 символов")
    private String position;

    @Size(max = 50, message = "Телефон не может быть длиннее 50 символов")
    private String phone;

    @Email(message = "Некорректный email")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов")
    private String email;

    private Long workshopId;

    @DecimalMin(value = "0", message = "Процент не может быть отрицательным")
    private java.math.BigDecimal managerCashPercent;

    /** ID компании */
    private Long companyId;
}
