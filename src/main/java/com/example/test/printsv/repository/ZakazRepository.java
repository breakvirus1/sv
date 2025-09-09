package com.example.test.printsv.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;


public interface ZakazRepository extends JpaRepository<Zakaz, Long> {
Page<Zakaz> findByUserOfZakaz(User user, Pageable pageable);

    @Query("SELECT SUM(z.sum) FROM Zakaz z WHERE z.userOfZakaz = :user AND z.createdAt BETWEEN :start AND :end")
    Integer findSumByUserAndPeriod(@Param("user") User user, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Search examples
    List<Zakaz> findBySumGreaterThan(Integer sum);
    List<Zakaz> findByUserOfZakaz(User userOfZakaz);

    @Query("SELECT z FROM Zakaz z WHERE LOWER(z.customerOfZakaz.name) = LOWER(:name)")
    List<Zakaz> findByCustomerOfZakazIgnoreCase(@Param("name") String name);

}