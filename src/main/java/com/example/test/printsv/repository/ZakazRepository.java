package com.example.test.printsv.repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.entity.Zakaz;
import java.util.List;


@Repository
public interface ZakazRepository extends JpaRepository<Zakaz, Long> {
    List<Zakaz> findByCustomerEmail(Customer customer);
    List<Zakaz> findByMaterialName(String materialName);
    List<Zakaz> findByUserEmail(String userEmail);
    
}
