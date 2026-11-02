package com.example.employeeservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Запрос на создание учётной записи сотрудника в Keycloak (вместе с ролью). */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakUserCreateRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(max = 100, message = "Логин не может быть длиннее 100 символов")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    @NotBlank(message = "ФИО обязательно")
    @Size(max = 255, message = "ФИО не может быть длиннее 255 символов")
    private String fullName;

    @Email(message = "Некорректный email")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов")
    private String email;

    /** Роль realm print-sv: ADMIN, MANAGER, PRODUCTION, ACCOUNTANT, USER или GOD. */
    @NotBlank(message = "Роль обязательна")
    private String role;

    /**
     * Компания, к которой привязывается пользователь (атрибут company_id).
     * Для роли ADMIN/GOD из JWT, явное значение учитывается только у GOD.
     */
    private Long companyId;
}
