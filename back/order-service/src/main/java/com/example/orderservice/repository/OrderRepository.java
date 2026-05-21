package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.totalAmount = :amount WHERE o.id = :id")
    void updateTotalAmount(Long id, java.math.BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.paidAmount = :amount WHERE o.id = :id")
    void updatePaidAmount(Long id, java.math.BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.debtAmount = o.totalAmount - o.paidAmount WHERE o.id = :id")
    void updateDebtAmount(Long id);

    @EntityGraph(attributePaths = {"client", "manager"})
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber AND o.deleted = false")
    Order findByOrderNumber(@Param("orderNumber") String orderNumber);

    @EntityGraph(attributePaths = {"client", "manager"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);
}
