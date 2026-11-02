package com.example.employeeservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на смену пароля существующего пользователя Keycloak.
 * Текущий пароль не запрашивается: Keycloak хранит только хеш, прочитать его нельзя — пароль можно только сбросить.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakPasswordUpdateRequest {

    @NotBlank(message = "Логин обязателен")
    @Size(max = 100, message = "Логин не может быть длиннее 100 символов")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    /** true — временный пароль: Keycloak потребует сменить его при первом входе. */
    private Boolean temporary;
}
