package com.example.test.printsv.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Zakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "sub_zakaz_id")
    private List<SubZakaz> subZakaz;

    @Column(nullable = false)
    private Integer sum;
    @OneToOne
    @JoinColumn(name = "user_of_zakaz_id", nullable = false)
    private User userOfZakaz;
    @OneToOne
    @JoinColumn(name = "customer_of_zakaz_id", nullable = false)
    private Customer customerOfZakaz;

}