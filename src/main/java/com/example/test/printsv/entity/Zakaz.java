package com.example.test.printsv.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "customer_id", nullable = false)
    // private Customer customerId;

//    public void addSubZakaz(SubZakaz subZakaz) {
//        subZakazList.add(subZakaz);
//        subZakaz.setZakaz(this);
//    }
//
//    public void removeSubZakaz(SubZakaz subZakaz) {
//        subZakazList.remove(subZakaz);
//        subZakaz.setZakaz(null);
//    }
}