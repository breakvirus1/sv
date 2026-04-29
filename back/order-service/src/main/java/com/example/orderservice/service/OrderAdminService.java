package com.example.orderservice.service;

import com.example.common.dto.OrderCreateRequest;
import com.example.common.dto.OrderItemCreateRequest;
import com.example.common.dto.OrderDto;
import com.example.common.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderAdminService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;
    private Random random = new Random();

    public List<Order> generateTestOrders(int count) {
        // Fetch IDs of existing clients, employees, and materials
        List<Long> clientIds = jdbcTemplate.query("SELECT id FROM clients WHERE deleted = false", (rs, rowNum) -> rs.getLong("id"));
        List<Long> employeeIds = jdbcTemplate.query("SELECT id FROM employees WHERE deleted = false", (rs, rowNum) -> rs.getLong("id"));
        List<MaterialDto> materials = jdbcTemplate.query(
            "SELECT id, name, unit, price FROM materials WHERE deleted = false",
            (rs, rowNum) -> new MaterialDto(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("price")
            )
        );

        if (clientIds.isEmpty() || employeeIds.isEmpty()) {
            throw new RuntimeException("Необходимо сначала сгенерировать клиентов и сотрудников");
        }

        List<Order> orders = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            OrderCreateRequest request = new OrderCreateRequest();
            request.setOrderNumber("З-" + String.format("%03d", i + 1));
            request.setClientId(clientIds.get(random.nextInt(clientIds.size())));
            request.setOrderDate(LocalDate.now().minusDays(random.nextInt(30)));
            request.setDueDate(LocalDate.now().plusDays(random.nextInt(60) + 7));
            request.setManagerId(employeeIds.get(random.nextInt(employeeIds.size())));

            // Create 1-3 items
            java.util.List<OrderItemCreateRequest> items = new ArrayList<>();
            int itemCount = 1 + random.nextInt(3);
            for (int j = 0; j < itemCount; j++) {
                OrderItemCreateRequest item = new OrderItemCreateRequest();
                if (!materials.isEmpty()) {
                    MaterialDto mat = materials.get(random.nextInt(materials.size()));
                    item.setName(mat.getName());
                    item.setPrice(mat.getPrice());
                } else {
                    item.setName("Изделие " + (j + 1));
                    item.setPrice(BigDecimal.valueOf(1000 + random.nextInt(9000)));
                }
                item.setQuantity(1 + random.nextInt(5));
                item.setReadyDate(LocalDate.now().plusDays(random.nextInt(30) + 7));
                items.add(item);
            }
            request.setItems(items);

            OrderDto orderDto = orderService.createOrder(request);
            // Fetch the saved order entity by ID to return
            Order order = orderRepository.findById(orderDto.getId()).orElseThrow();
            orders.add(order);
        }

        return orders;
    }

    // Simple DTO for material data
    private static class MaterialDto {
        private final Long id;
        private final String name;
        private final String unit;
        private final BigDecimal price;

        public MaterialDto(Long id, String name, String unit, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.unit = unit;
            this.price = price;
        }

        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
    }
}
