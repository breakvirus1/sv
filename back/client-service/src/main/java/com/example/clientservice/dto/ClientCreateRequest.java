package com.example.clientservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO для создания нового клиента.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientCreateRequest {
    /** Наименование клиента */
    @NotBlank(message = "Название клиента обязательно")
    @Size(max = 255, message = "Название клиента не может быть длиннее 255 символов")
    private String name;
    /** Тип: PRIVATE (частник) или COMPANY (компания) */
    @Size(max = 20, message = "Тип клиента не может быть длиннее 20 символов")
    private String type;
    /** Контактное лицо */
    @Size(max = 255, message = "Контактное лицо не может быть длиннее 255 символов")
    private String contactPerson;
    /** Телефон */
    @Size(max = 50, message = "Телефон не может быть длиннее 50 символов")
    private String phone;
    /** Email */
    @Email(message = "Некорректный email")
    @Size(max = 100, message = "Email не может быть длиннее 100 символов")
    private String email;
    /** ИНН */
    @Size(max = 20, message = "ИНН не может быть длиннее 20 символов")
    private String inn;
    /** Адрес */
    @Size(max = 255, message = "Адрес не может быть длиннее 255 символов")
    private String address;
    /** Процент добавки к сумме заказа */
    @DecimalMin(value = "0", message = "Процент добавки не может быть отрицательным")
    private BigDecimal priceplus;
    /** ID компании */
    private Long companyId;
}
