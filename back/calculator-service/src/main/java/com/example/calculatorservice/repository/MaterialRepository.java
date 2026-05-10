 package com.example.calculatorservice.repository;

 import com.example.calculatorservice.entity.Banner;
 import com.example.calculatorservice.entity.Material;
 import com.example.calculatorservice.entity.MaterialType;
 import com.example.calculatorservice.entity.Plenka;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;

 import java.util.List;
 import java.util.stream.Collectors;

 @Repository
 public interface MaterialRepository extends JpaRepository<Material, Long> {

     default List<Material> findByType(MaterialType type) {
         return findAll().stream()
                 .filter(m -> {
                     if (type == MaterialType.BANNER) return m instanceof Banner;
                     if (type == MaterialType.PLENKA) return m instanceof Plenka;
                     return false;
                 })
                 .collect(Collectors.toList());
     }
 }
