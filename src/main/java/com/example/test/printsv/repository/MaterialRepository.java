package com.example.test.printsv.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.Material;

public interface MaterialRepository extends JpaRepository<Material,Long>{
    Optional<Material> findByName(String name);

}
