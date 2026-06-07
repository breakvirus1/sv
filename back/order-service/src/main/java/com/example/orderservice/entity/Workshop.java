package com.example.orderservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workshops")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Workshop extends BaseEntity {

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ElementCollection
    @CollectionTable(name = "workshop_operations", joinColumns = @JoinColumn(name = "workshop_id", referencedColumnName = "id"))
    @Column(name = "operation_id")
    private List<Long> operationIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "workshop_materials", joinColumns = @JoinColumn(name = "workshop_id", referencedColumnName = "id"))
    @Column(name = "material_id")
    private List<Long> materialIds = new ArrayList<>();

    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStage> orderStages = new ArrayList<>();
}
