package com.example.clientservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO для клиента (читающее представление).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientResponse {
    /** ID клиента */
    private Long id;
    /** Наименование клиента */
    private String name;
    /** Тип: PRIVATE (частник) или COMPANY (компания) */
    private String type;
    /** Контактное лицо */
    private String contactPerson;
    /** Телефон */
    private String phone;
    /** Email */
    private String email;
    /** ИНН */
    private String inn;
    /** Адрес */
    private String address;
    /** Процент добавки к сумме заказа */
    private BigDecimal priceplus;
}
