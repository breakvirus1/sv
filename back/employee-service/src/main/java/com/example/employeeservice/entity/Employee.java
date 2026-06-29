package com.example.employeeservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность "Сотрудник" — исполнитель или менеджер.
 * Примеры: "Мохирёва Наталья", "Шипилов Станислав".
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Employee extends BaseEntity {

    /** Полное имя сотрудника (ФИО) */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** Логин для связи с Keycloak (sub или preferred_username) */
    @Column(name = "username", length = 100, unique = true)
    private String username;

    /** Должность: Менеджер, Печатник, Бухгалтер и т.д. */
    @Column(name = "position", length = 100)
    private String position;

    /** Контактный телефон */
    @Column(length = 50)
    private String phone;

    /** Email адрес */
    @Column(length = 255)
    private String email;

    /** ID цеха, к которому привязан сотрудник */
    @Column(name = "workshop_id")
    private Long workshopId;

    /** Процент заработка менеджера от priceplus (настраивается админом) */
    @Column(name = "manager_cash_percent", precision = 5, scale = 2)
    private java.math.BigDecimal managerCashPercent;

    /** ID основной роли из public.roles (синхронизируется из Keycloak) */
    @Column(name = "role_id")
    private Long roleId;

    /** Роли сотрудника (синхронизируются из Keycloak) */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "employee_roles",
        schema = "svschema",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}
