// package com.example.test.printsv.entity;

// import org.springframework.data.annotation.Id;

// import jakarta.persistence.*;
// import lombok.*;

// @Entity
// @Setter
// @Getter
// @AllArgsConstructor
// @NoArgsConstructor
// @Table(name="subZakaz")
// public class SubZakaz {
//     @Id
//     @GeneratedValue(strategy = GenerationType.IDENTITY)
//     private Long id;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "material_name", nullable = false)
//     private Material material;

//     @Column(nullable = false)
//     private Double length;

//     @Column(nullable = false)
//     private Double width;

//     @Column
//     private String filePath;

//     @Column
//     private String comment;

//     @Column(nullable = false)
//     private Integer cena;

//     @Enumerated(EnumType.STRING)
//     @Column
//     private boolean done;

//     @ManyToOne(fetch = FetchType.LAZY)
//     @JoinColumn(name = "zakaz_id")
//     private Zakaz zakaz;
// }


package com.example.test.printsv.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "subZakaz")
public class SubZakaz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_name", nullable = false)
    private Material material;

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
    private Boolean done; // Removed @Enumerated, kept as Boolean

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zakaz_id")
    private Zakaz zakaz;
}