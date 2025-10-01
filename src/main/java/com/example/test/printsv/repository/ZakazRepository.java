package com.example.test.printsv.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.Zakaz;


public interface ZakazRepository extends JpaRepository<Zakaz, Long> {

}