package com.example.test.printsv.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.response.ZakazResponse;

import java.util.List;


@Repository
public interface ZakazRepository extends JpaRepository<Zakaz, Long> {

    @Query(value = "SELECT * FROM Customer WHERE name = :value", nativeQuery = true)
    List<ZakazResponse> findByCustomerNameIgnoreCase(String customerName);

    
}
