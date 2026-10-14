package com.example.clientservice.repository;

import com.example.clientservice.entity.Company;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long>, JpaSpecificationExecutor<Company> {
    Optional<Company> findByName(String name);

    @Cacheable(value = "companies", key = "'all'")
    @Query("SELECT c FROM Company c WHERE c.deleted = false")
    List<Company> findAllActive();
}
