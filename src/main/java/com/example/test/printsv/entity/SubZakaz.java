package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subZakaz")
public class SubZakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double length;

    @Column(nullable = false)
    private Double width;

    @Column
    private String filePath;

    @Column
    private String comment;

    @Column(nullable = false)
    private Integer cena;

    @Column
    private Boolean done;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zakaz_id")
    private Zakaz zakaz;
}