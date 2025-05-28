package com.example.test.printsv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.Material;

public interface MaterialRepository extends JpaRepository<Material,Long>{
    List<Material> findByMaterialName(String materialName);

}
