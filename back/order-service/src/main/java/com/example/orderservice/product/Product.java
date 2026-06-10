package com.example.orderservice.product;

import com.example.orderservice.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    private String name;
    private String article;
    private String description;

    private BigDecimal width;
    private BigDecimal height;

    private String unit = "шт";
    private BigDecimal basePrice;

    /** Категория изделия (Вывески, Баннеры, Объемные буквы...) */
    private String category;

    /** Флаг активности шаблона */
    private Boolean isActive = true;

    /** Формула расчёта в JSONB (хранит сложную логику) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String formulaJson;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ProductMaterial> materials = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private List<ProductOperation> operations = new ArrayList<>();
}
