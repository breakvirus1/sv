package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность "Роль" — связь пользователя с ролью в системе.
 * Хранится в таблице roles, используется для авторизации через Keycloak.
 * Связана с User через ManyToMany.
 */
@Entity
@Table(name = "roles")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название роли (значение из ERole) */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    /**
     * Возвращает строковое представление роли.
     * Например, ROLE_ADMIN, ROLE_MANAGER.
     */
    public String getName() {
        return name != null ? name.name() : null;
}

    @Override
    public String toString() {
        return "{" +
                " id='" + getId() + "'" +
                ", name='" + getName() + "'" +
                "}";
    }
}
