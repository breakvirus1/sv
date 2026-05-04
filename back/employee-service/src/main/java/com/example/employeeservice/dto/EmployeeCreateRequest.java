package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO для создания нового сотрудника.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeCreateRequest {
    /** Полное имя (ФИО) */
    private String fullName;
    /** Логин */
    private String username;
    /** Должность (Менеджер, Печатник, Бухгалтер и т.д.) */
    private String position;
    /** Контактный телефон */
    private String phone;
    /** Email адрес */
    private String email;
}
