package com.example.test.printsv.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.Customer;


public interface CustomerRepository extends JpaRepository<Customer,Long>{
    List<Customer> findByEmail(String email);
    List<Customer> findByZakazId (Long id);

    


}
