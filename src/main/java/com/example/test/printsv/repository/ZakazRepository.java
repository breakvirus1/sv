package com.example.test.printsv.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.test.printsv.entity.Zakaz;


public interface ZakazRepository extends JpaRepository<Zakaz, Long> {
    @Query("SELECT z FROM Zakaz z WHERE z.user.id = :userId")
    List<Zakaz> findAllByUserId(@Param("userId") Long userId);
    

}