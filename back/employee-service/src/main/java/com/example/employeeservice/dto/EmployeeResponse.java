package com.example.employeeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO для сотрудника (исполнителя/менеджера).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeResponse {
    /** ID сотрудника */
    private Long id;
    /** Полное имя (ФИО) */
    private String fullName;
    /** Должность (Менеджер, Печатник, Бухгалтер и т.д.) */
    private String position;
    /** Контактный телефон */
    private String phone;
    /** Email адрес */
    private String email;
    /** Логин (username) */
    private String username;
}
