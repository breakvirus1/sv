package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "files")
@Getter @Setter @AllArgsConstructor @NoArgsConstructor
@Cache(region = "FileAttachment", usage = CacheConcurrencyStrategy.READ_WRITE)
public class FileAttachment extends BaseEntity {

    /** Уникальное имя файла в хранилище (номерЗаказа_менеджер_материал_операции_параметры.расширение) */
    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    /** Путь к файлу в хранилище */
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
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    /** Кто загрузил файл (логин пользователя) */
    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;
}
