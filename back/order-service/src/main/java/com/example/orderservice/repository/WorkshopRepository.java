package com.example.orderservice.repository;

import com.example.orderservice.entity.Workshop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface WorkshopRepository extends JpaRepository<Workshop, Long>, JpaSpecificationExecutor<Workshop> {
    List<Workshop> findAllByOrderBySortOrderAsc();
}
