package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sub_zakaz")
public class SubZakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String material;
    private Double width;
    private Double height;
    private Double cena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zakaz_id", nullable = false)
    private Zakaz zakaz;

    public Double getTotal() {
        return width * height * cena;
    }
}