package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

    @OneToMany(mappedBy = "zakaz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<SubZakaz> subZakazList = new ArrayList<>();

    @Column(nullable = false)
    private Integer sum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userOfZakaz_id", nullable = false)
    @JsonBackReference
    private User userOfZakaz;
    
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customerOfZakaz_id", nullable = false)
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