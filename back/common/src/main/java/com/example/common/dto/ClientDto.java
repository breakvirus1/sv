package com.example.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для клиента (читающее представление).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientDto {
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
}
