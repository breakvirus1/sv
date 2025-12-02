package com.example.test.printsv.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;
    private Double width;
    private Double height;
    private Double cena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zakaz_id", nullable = false)
    @JsonIgnore
    private Zakaz zakaz;


}