package com.example.test.printsv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.Zakaz;


import java.util.List;
import java.util.Optional;

@Repository
public interface SubZakazRepository extends JpaRepository<SubZakaz, Long> {
    // Find zakaz by specific status
    List<Zakaz> findByStatus(boolean statusDone);


    // Custom query to find zakaz within a date range
    @Query("SELECT z FROM Zakaz z WHERE z.createdDate BETWEEN :startDate AND :endDate")
    List<Zakaz> findZakazBetweenDates(
        @Param("startDate") java.util.Date startDate, 
        @Param("endDate") java.util.Date endDate
    );

    // Find active zakaz by ID
    Optional<Zakaz> findByIdAndStatusNot(Long id, boolean statusDone);

    // Count zakaz by specific status
    long countByStatus(boolean statusDone);
}
