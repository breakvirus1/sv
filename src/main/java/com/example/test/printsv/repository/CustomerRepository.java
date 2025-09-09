package com.example.test.printsv.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.entity.Zakaz;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByNameContaining(String name);
    Customer findByPhoneContaining(String phone);

}
