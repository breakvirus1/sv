package com.example.test.printsv.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "zakaz")
public class Zakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // @Column(nullable = false)
    private Integer sum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;
    
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "zakaz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<SubZakaz> subZakazList;

    public void addSubZakaz(SubZakaz subZakaz) {
        subZakazList.add(subZakaz);
        subZakaz.setZakaz(this);
    }

    public void removeSubZakaz(SubZakaz subZakaz) {
        subZakazList.remove(subZakaz);
        subZakaz.setZakaz(null);
    }
}