package com.example.test.printsv.entity;

import org.springframework.data.annotation.Id;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubZakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="material_name", nullable = false)
    private Material material;
    @Column(nullable = false)
    private Double lenght;
    @Column(nullable = false)
    private Double width;
    @Column
    private String filePath;
    @Column
    private String comment;
    @Column(nullable = false)
    private Integer cena;
    @Enumerated(EnumType.STRING)
    @Column
    private boolean done;
}
