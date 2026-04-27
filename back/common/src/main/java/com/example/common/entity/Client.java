package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Сущность "Клиент" — физическое лицо (частник) или компания.
 * Примеры: "Частник", "Монолит-1", "Компания Высота".
 * Связан с заказами (OneToMany).
 */
@Entity
@Table(name = "clients")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Client extends BaseEntity {

    /** Наименование клиента */
    @Column(nullable = false, length = 255)
    private String name;

    /** Тип клиента: PRIVATE (частник) или COMPANY (компания) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private ClientType type = ClientType.PRIVATE;

    /** Контактное лицо (ФИО) */
    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    /** Контактный телефон */
    @Column(length = 50)
    private String phone;

    /** Email адрес */
    @Column(length = 255)
    private String email;

    /** ИНН (идентификационный номер налогоплательщика, для компаний) */
    @Column(length = 100)
    private String inn;

    /** Юридический/почтовый адрес */
    @Column(length = 255)
    private String address;

    /** Список заказов, принадлежащих этому клиенту */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders = new ArrayList<>();
}
