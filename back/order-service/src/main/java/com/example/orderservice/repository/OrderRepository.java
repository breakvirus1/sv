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

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.totalAmount = ?2 WHERE o.id = ?1")
    void updateTotalAmount(Long id, BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.paidAmount = ?2 WHERE o.id = ?1")
    void updatePaidAmount(Long id, BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.debtAmount = o.totalAmount - o.paidAmount WHERE o.id = ?1")
    void updateDebtAmount(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.costPrice = ?2 WHERE o.id = ?1")
    void updateCostPrice(Long id, BigDecimal amount);

    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.marginPercent = ?2 WHERE o.id = ?1")
    void updateMarginPercent(Long id, BigDecimal percent);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.client " +
           "LEFT JOIN FETCH o.manager " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.operations " +
           "LEFT JOIN FETCH o.materials m " +
           "LEFT JOIN FETCH m.material " +
           "LEFT JOIN FETCH m.operations " +
           "LEFT JOIN FETCH o.stages " +
           "LEFT JOIN FETCH o.payments " +
           "LEFT JOIN FETCH o.comments " +
           "WHERE o.id = ?1")
    Optional<Order> findByIdWithAllDetails(Long id);

    Optional<Order> findByOrderNumber(String orderNumber);
}
