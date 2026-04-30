package com.example.orderservice.service;

import com.example.common.dto.OrderCreateRequest;
import com.example.common.dto.OrderMaterialCreateRequest;
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
            // Номер заказа генерируется автоматически в OrderService
            request.setClientId(clientIds.get(random.nextInt(clientIds.size())));
            request.setOrderDate(LocalDate.now().minusDays(random.nextInt(30)));
            request.setDueDate(LocalDate.now().plusDays(random.nextInt(60) + 7));
            request.setManagerId(employeeIds.get(random.nextInt(employeeIds.size())));

            // Создаем 1-3 позиции заказа (материалы)
            java.util.List<OrderMaterialCreateRequest> items = new ArrayList<>();
            int itemCount = 1 + random.nextInt(3);
            for (int j = 0; j < itemCount; j++) {
                if (!materials.isEmpty()) {
                    MaterialDto mat = materials.get(random.nextInt(materials.size()));
                    BigDecimal quantity;
                    // Определяем количество в зависимости от единицы измерения
                    if ("м2".equals(mat.getUnit())) {
                        int dim1 = 500 + random.nextInt(2500); // ширина в мм
                        int dim2 = 500 + random.nextInt(2500); // высота в мм
                        // quantity в м2 = (dim1/1000) * (dim2/1000)
                        quantity = BigDecimal.valueOf(dim1 / 1000.0)
                                .multiply(BigDecimal.valueOf(dim2 / 1000.0))
                                .setScale(3, java.math.RoundingMode.HALF_UP);
                    } else if ("м.п.".equals(mat.getUnit())) {
                        int dim = 500 + random.nextInt(5000); // длина в мм
                        quantity = BigDecimal.valueOf(dim / 1000.0).setScale(3, java.math.RoundingMode.HALF_UP);
                    } else {
                        // Для других единиц (например, штуки) — случайное целое число
                        quantity = BigDecimal.valueOf(1 + random.nextInt(20));
                    }
                    OrderMaterialCreateRequest om = new OrderMaterialCreateRequest();
                    om.setMaterialId(mat.getId());
                    om.setQuantity(quantity);
                    om.setReadyDate(LocalDate.now().plusDays(random.nextInt(30) + 7));
                    items.add(om);
                }
            }
            request.setItems(items);

            OrderDto orderDto = orderService.createOrder(request);
            // Получаем сохраненную сущность заказа для возврата
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

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public BigDecimal getPrice() { return price; }
    }
}
