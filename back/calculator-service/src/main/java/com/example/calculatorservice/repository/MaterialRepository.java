package com.example.calculatorservice.repository;

import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.MaterialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    default List<Material> findByType(MaterialType type) {
        String keyword = (type == MaterialType.BANNER) ? "баннер" : "плёнка";
        return findAll().stream()
                .filter(m -> {
                    String name = m.getName();
                    return name != null && name.toLowerCase().contains(keyword);
                })
                .collect(Collectors.toList());
    }

    Optional<Material> findByIdAndDeletedFalse(Long id);
}
