package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "zakaz")
public class Zakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "zakaz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubZakaz> subZakazList = new ArrayList<>();

    @Column(nullable = false)
    private Integer sum;

    @OneToOne
    @JoinColumn(name = "user_of_zakaz_id", nullable = false)
    private User userOfZakaz;

    @OneToOne
    @JoinColumn(name = "customer_of_zakaz_id", nullable = false)
    private Customer customerOfZakaz;

    public void addSubZakaz(SubZakaz subZakaz) {
        subZakazList.add(subZakaz);
        subZakaz.setZakaz(this);
    }

    public void removeSubZakaz(SubZakaz subZakaz) {
        subZakazList.remove(subZakaz);
        subZakaz.setZakaz(null);
    }
}