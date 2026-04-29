package com.example.orderservice.repository;

import com.example.common.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

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
}
