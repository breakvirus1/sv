package com.example.orderservice.service;

import com.example.common.dto.*;
import com.example.common.entity.Client;
import com.example.common.entity.Employee;
import com.example.common.entity.Material;
import com.example.common.entity.Order;
import com.example.common.entity.OrderComment;
import com.example.common.dto.OrderMaterialCreateRequest;
import com.example.common.entity.OrderItem;
import com.example.common.entity.OrderMaterial;
import com.example.common.entity.OrderStatus;
import com.example.common.entity.Payment;
import com.example.common.entity.ProductionStage;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderCommentRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderStageRepository;
import com.example.orderservice.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис для управления заказами.
 * Обрабатывает бизнес-логику создания, обновления и получения заказов.
 * Включает пересчет сумм, управление статусами и стадиями производства.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStageRepository orderStageRepository;
    private final PaymentRepository paymentRepository;
    private final OrderCommentRepository orderCommentRepository;
    private final OrderMapper orderMapper;
    @PersistenceContext
    private EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Получить список заказов с фильтрацией и пагинацией.
     */
    public Page<OrderDto> getAllOrders(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toDto);
    }

    /**
     * Получить детальную информацию о заказе со всеми связанными сущностями.
     */
    @Transactional(readOnly = true)
    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        OrderDto dto = orderMapper.toDto(order);

        // Загружаем связанные сущности
        dto.setItems(order.getItems().stream()
                .map(orderMapper::itemToDto)
                .collect(Collectors.toList()));

        dto.setStages(order.getStages().stream()
                .map(orderMapper::stageToDto)
                .collect(Collectors.toList()));

        dto.setPayments(order.getPayments().stream()
                .map(this::mapPayment)
                .collect(Collectors.toList()));

        dto.setComments(order.getComments().stream()
                .map(this::mapComment)
                .collect(Collectors.toList()));

        dto.setMaterials(order.getMaterials().stream()
                .map(this::mapOrderMaterial)
                .collect(Collectors.toList()));

        return dto;
    }

    /**
     * Создать новый заказ с позициями (материалами).
     * Номер заказа генерируется автоматически.
     * Общая сумма рассчитывается на основе стоимости материалов с учетом коэффициента отхода.
     */
    public OrderDto createOrder(OrderCreateRequest request) {
        // Генерация номера заказа
        String generatedOrderNumber = generateOrderNumber();

        Order order = new Order();
        order.setOrderNumber(generatedOrderNumber);
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        order.setDueDate(request.getDueDate());

        // Клиент
        Client client = entityManager.find(Client.class, request.getClientId());
        if (client == null) {
            throw new RuntimeException("Клиент не найден");
        }
        order.setClient(client);

        // Менеджер
        if (request.getManagerId() != null) {
            Employee manager = entityManager.find(Employee.class, request.getManagerId());
            if (manager == null) {
                throw new RuntimeException("Менеджер не найден");
            }
            order.setManager(manager);
        }

        order.setDescription(request.getDescription());
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDebtAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.WAITING);
        order.setProductionStage(ProductionStage.NOT_STARTED);

        Order saved = orderRepository.save(order);

        // Обработка позиций заказа (материалы)
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (OrderMaterialCreateRequest matReq : request.getItems()) {
                Material material = entityManager.find(Material.class, matReq.getMaterialId());
                if (material == null) {
                    throw new RuntimeException("Материал не найден: " + matReq.getMaterialId());
                }

                OrderMaterial orderMaterial = new OrderMaterial();
                orderMaterial.setOrder(saved);
                orderMaterial.setMaterial(material);
                orderMaterial.setQuantity(matReq.getQuantity());
                orderMaterial.setWasteCoefficient(material.getWasteCoefficient());
                orderMaterial.setReadyDate(matReq.getReadyDate());
                // Расчет стоимости: цена * количество * коэффициент отхода
                BigDecimal cost = material.getPrice()
                        .multiply(matReq.getQuantity())
                        .multiply(material.getWasteCoefficient());
                orderMaterial.setCost(cost);

                saved.getMaterials().add(orderMaterial);
                total = total.add(cost);
            }
            saved.setTotalAmount(total);
            // Cascade persist materials
            orderRepository.save(saved);
        }

        return getOrderById(saved.getId());
    }

    /**
     * Сгенерировать уникальный номер заказа.
     * Формат: ORD-{timestamp}
     */
    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    /**
     * Обновить статус заказа.
     * При смене статуса могут выполняться дополнительные действия (уведомления, триггеры).
     */
    public OrderDto updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        order.setStatus(OrderStatus.valueOf(status));
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    /**
     * Обновить стадию производства.
     * Используется для отслеживания прогресса в цехах.
     */
    public OrderDto updateProductionStage(Long id, String stage) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        order.setProductionStage(ProductionStage.valueOf(stage));
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    /**
     * Добавить оплату к заказу.
     * Автоматически пересчитывает paidAmount и debtAmount.
     */
    public void addPayment(Long orderId, PaymentDto paymentDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(paymentDto.getAmount());
        payment.setPaymentDate(paymentDto.getPaymentDate() != null ? paymentDto.getPaymentDate() : LocalDate.now());
        payment.setPaymentType(paymentDto.getPaymentType());
        payment.setDetails(paymentDto.getDetails());
        payment.setIsPartial(paymentDto.getIsPartial() != null ? paymentDto.getIsPartial() : false);

        paymentRepository.save(payment);

        // Пересчитаем оплаченную сумму и долг
        recalculatePaidAmount(orderId);
    }

    /**
     * Пересчитать общую сумму заказа на основе позиций.
     */
    private void recalculateTotalAmount(Long orderId) {
        BigDecimal total = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(cost), 0) FROM order_items WHERE order_id = ? AND deleted = false",
            new Object[]{orderId},
            BigDecimal.class
        );
        orderRepository.updateTotalAmount(orderId, total);
        // Also update debt
        orderRepository.updateDebtAmount(orderId);
    }

    /**
     * Пересчитать сумму оплат и долг.
     */
    private void recalculatePaidAmount(Long orderId) {
        BigDecimal paid = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE order_id = ? AND deleted = false",
            new Object[]{orderId},
            BigDecimal.class
        );
        orderRepository.updatePaidAmount(orderId, paid);
        // Also update debt
        orderRepository.updateDebtAmount(orderId);
    }

    /**
     * Преобразовать сущность Payment в DTO.
     */
    private PaymentDto mapPayment(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentType(),
                payment.getDetails(),
                payment.getIsPartial()
        );
    }

    /**
     * Преобразовать сущность OrderComment в CommentDto.
     */
    private CommentDto mapComment(OrderComment comment) {
        Employee author = comment.getAuthor();
        EmployeeDto authorDto = author != null ?
                new EmployeeDto(author.getId(), author.getFullName(), author.getPosition(), author.getPhone(), author.getEmail()) :
                null;

        return new CommentDto(
                comment.getId(),
                comment.getMessage(),
                authorDto,
                comment.getIsInternal(),
                comment.getCreatedAt()
        );
    }

    private OrderMaterialDto mapOrderMaterial(OrderMaterial om) {
        Material material = om.getMaterial();
        MaterialDto materialDto = material != null ?
                new MaterialDto(material.getId(), material.getName(), material.getUnit(), material.getPrice(), material.getWasteCoefficient()) :
                null;

        return new OrderMaterialDto(
                om.getId(),
                materialDto,
                om.getQuantity(),
                om.getReadyDate(),
                om.getWasteCoefficient(),
                om.getCost()
        );
    }
}
