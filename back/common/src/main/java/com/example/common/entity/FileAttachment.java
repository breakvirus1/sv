package com.example.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Сущность "Файл" — вложение к заказу или позиции.
 * Файлы могут быть исходными макетами, доказательствами выполнения и т.д.
 * Хранятся в MinIO/S3, здесь — только метаданные.
 */
@Entity
@Table(name = "files")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileAttachment extends BaseEntity {

    /** Уникальное имя файла в хранилище (UUID + расширение) */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** Оригинальное имя файла (как загрузил пользователь) */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /** Путь к файлу в хранилище (ключ в MinIO/S3) */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** Публичный URL для доступа к файлу (если нужно) */
    @Column(name = "file_url", length = 500)
    private String fileUrl;

    /** MIME-type файла (image/png, application/pdf и т.д.) */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** Размер файла в байтах */
    @Column(name = "file_size")
    private Long fileSize;

    /** Заказ, к которому прикреплен файл (может быть null, если файл привязан к позиции) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** Позиция заказа, к которой прикреплен файл (может быть null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    /** Кто загрузил файл (логин пользователя) */
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;
}
