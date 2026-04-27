package com.example.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Базовый класс для всех сущностей.
 * Предоставляет стандартные поля:
 * - id (идентификатор)
 * - createdAt (дата создания, заполняется автоматически)
 * - updatedAt (дата последнего обновления, заполняется автоматически)
 * - deleted (флаг мягкого удаления)
 *
 * Аннотация @EntityListeners автоматически обновляет createdAt и updatedAt
 * при сохранении и обновлении сущности через AuditingEntityListener.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /** Уникальный идентификатор (автоинкремент) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Дата и время создания записи (заполняется автоматически) */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Дата и время последнего обновления (заполняется автоматически) */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Флаг мягкого удаления (soft delete) */
    @Column(name = "deleted")
    private Boolean deleted = false;
}
