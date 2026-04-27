package com.example.orderservice.service;

import com.example.common.dto.*;
import com.example.common.entity.Employee;
import com.example.common.entity.Material;
import com.example.common.entity.Order;
import com.example.common.entity.OrderComment;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * Создать новый заказ с позициями.
     * Автоматически рассчитывает общую сумму.
     */
    public OrderDto createOrder(OrderCreateRequest request) {
        Order order = new Order();
        order.setOrderNumber(request.getOrderNumber());
        order.setOrderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now());
        order.setDueDate(request.getDueDate());

        // Здесь нужно загрузить Client и Employee по ID (ленивая загрузка)
        // Для простоты оставляем null, в реальности используем repositories

        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDebtAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.WAITING);
        order.setProductionStage(ProductionStage.NOT_STARTED);

        Order saved = orderRepository.save(order);

        // Если есть позиции - создать их
        if (request.getItems() != null) {
            request.getItems().forEach(itemReq -> {
                OrderItem item = new OrderItem();
                item.setOrder(saved);
                item.setName(itemReq.getName());
                item.setPrice(itemReq.getPrice());
                item.setQuantity(itemReq.getQuantity());
                item.setCost(itemReq.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
                item.setReadyDate(itemReq.getReadyDate());
                orderItemRepository.save(item);
            });

            // Пересчитать общую сумму
            recalculateTotalAmount(saved.getId());
        }

        return getOrderById(saved.getId());
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
        // Реализация: агрегация order_items.cost
        // Заглушка — нужно реализовать через repository или JPQL
    }

    /**
     * Пересчитать сумму оплат и долг.
     */
    private void recalculatePaidAmount(Long orderId) {
        // Реализация: сумма всех payments по заказу
        // Заглушка — нужно реализовать
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
                om.getWasteCoefficient(),
                om.getCost()
        );
    }
}
