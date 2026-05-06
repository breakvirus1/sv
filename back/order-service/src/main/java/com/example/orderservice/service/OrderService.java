package com.example.orderservice.service;

import com.example.clientservice.entity.Client;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.entity.Employee;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderComment;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderMaterialOperation;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ProductionStage;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.order.entity.OrderItemMaterial;
import com.example.orderservice.order.entity.OrderItemOperation;
import com.example.orderservice.product.Product;
import com.example.orderservice.product.ProductMaterial;
import com.example.orderservice.product.ProductOperation;
import com.example.orderservice.product.repository.ProductRepository;
import com.example.orderservice.repository.*;
import com.example.orderservice.service.CalculatorService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ProductRepository productRepository;
    private final CalculatorService calculatorService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Получить список заказов с фильтрацией и пагинацией.
     */
    public Page<OrderResponse> getAllOrders(Specification<Order> spec, Pageable pageable) {
        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toDto);
    }

    /**
     * Получить детальную информацию о заказе со всеми связанными сущностями.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        OrderResponse response = orderMapper.toDto(order);

        // Загружаем связанные сущности
        response.setItems(order.getItems().stream()
                .map(orderMapper::itemToDto)
                .collect(Collectors.toList()));

        response.setStages(order.getStages().stream()
                .map(orderMapper::stageToDto)
                .collect(Collectors.toList()));

        response.setPayments(order.getPayments().stream()
                .map(this::mapPayment)
                .collect(Collectors.toList()));

        response.setComments(order.getComments().stream()
                .map(this::mapComment)
                .collect(Collectors.toList()));

        response.setMaterials(order.getMaterials().stream()
                .map(this::mapOrderMaterial)
                .collect(Collectors.toList()));

        return response;
    }

    /**
     * Создать новый заказ с позициями (материалами).
     * Номер заказа генерируется автоматически.
     * Общая сумма рассчитывается на основе стоимости материалов с учетом коэффициента отхода.
     */
    public OrderResponse createOrder(OrderCreateRequest request) {
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

                // Обработка операций для материала
                if (matReq.getOperations() != null && !matReq.getOperations().isEmpty()) {
                    for (OrderOperationCreateRequest opReq : matReq.getOperations()) {
                        MaterialOperation template = null;
                        if (opReq.getMaterialOperationId() != null) {
                            template = entityManager.find(MaterialOperation.class, opReq.getMaterialOperationId());
                            if (template == null) {
                                throw new RuntimeException("Операция не найдена: " + opReq.getMaterialOperationId());
                            }
                        }
                        OrderMaterialOperation orderOp = new OrderMaterialOperation();
                        orderOp.setOrder(saved);
                        orderOp.setOrderMaterial(orderMaterial);
                        orderOp.setTemplate(template);
                        orderOp.setName(template != null ? template.getName() : "Операция");
                        orderOp.setOperationType(template != null ? template.getOperationType().name() : null);
                        orderOp.setBasePrice(template != null ? template.getBasePrice() : BigDecimal.ZERO);
                        orderOp.setUnit(template != null ? template.getUnit() : "шт");
                        orderOp.setWasteCoefficient(
                                opReq.getWasteCoefficient() != null ? opReq.getWasteCoefficient() :
                                        (template != null ? template.getWasteCoefficient() : BigDecimal.ONE)
                        );
                        orderOp.setQuantity(opReq.getQuantity() != null ? opReq.getQuantity() : BigDecimal.ONE);
                        BigDecimal opCost = orderOp.getBasePrice().multiply(orderOp.getQuantity()).multiply(orderOp.getWasteCoefficient());
                        orderOp.setCost(opCost);
                        total = total.add(opCost);

                        // Add operation to material's operations list for cascade persist
                        orderMaterial.getOperations().add(orderOp);

                        // Стоимость дополнительных материалов
                        if (opReq.getAdditionalMaterials() != null && !opReq.getAdditionalMaterials().isEmpty()) {
                            for (Map.Entry<Long, BigDecimal> entry : opReq.getAdditionalMaterials().entrySet()) {
                                Material addMat = entityManager.find(Material.class, entry.getKey());
                                if (addMat != null) {
                                    BigDecimal addMatCost = addMat.getPrice().multiply(entry.getValue());
                                    total = total.add(addMatCost);
                                }
                            }
                        }
                    }
                }

                saved.getMaterials().add(orderMaterial);
                total = total.add(cost);
            }
            saved.setTotalAmount(total);
            // Cascade persist materials and their operations
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
    public OrderResponse updateStatus(Long id, String status) {
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
    public OrderResponse updateProductionStage(Long id, String stage) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        order.setProductionStage(ProductionStage.valueOf(stage));
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    /**
     * Обновить заказ.
     */
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        // Update simple fields
        if (request.getDescription() != null) {
            order.setDescription(request.getDescription());
        }
        if (request.getOrderDate() != null) {
            order.setOrderDate(request.getOrderDate());
        }
        if (request.getDueDate() != null) {
            order.setDueDate(request.getDueDate());
        }

        // Update manager if provided
        if (request.getManagerId() != null) {
            Employee manager = entityManager.find(Employee.class, request.getManagerId());
            if (manager == null) {
                throw new RuntimeException("Менеджер не найден");
            }
            order.setManager(manager);
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    /**
     * Добавить оплату к заказу.
     * Автоматически пересчитывает paidAmount и debtAmount.
     */
    public void addPayment(Long orderId, PaymentRequest paymentRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        com.example.orderservice.entity.Payment payment = new com.example.orderservice.entity.Payment();
        payment.setOrder(order);
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentDate(paymentRequest.getPaymentDate() != null ? paymentRequest.getPaymentDate() : LocalDate.now());
        payment.setPaymentType(paymentRequest.getPaymentType());
        payment.setDetails(paymentRequest.getDetails());
        payment.setIsPartial(paymentRequest.getIsPartial() != null ? paymentRequest.getIsPartial() : false);

        paymentRepository.save(payment);

        // Пересчитаем оплаченную сумму и долг
        recalculatePaidAmount(orderId);
    }

    /**
     * Добавить комментарий к заказу.
     */
    public CommentResponse addComment(Long orderId, CommentRequest commentRequest, EmployeeResponse author) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        OrderComment comment = new OrderComment();
        comment.setOrder(order);
        comment.setMessage(commentRequest.getMessage());
        comment.setIsInternal(commentRequest.getIsInternal() != null ? commentRequest.getIsInternal() : false);
        // Author would be set from authentication
        if (author != null) {
            Employee authorEntity = entityManager.find(Employee.class, author.getId());
            comment.setAuthor(authorEntity);
        }
        comment.setCreatedAt(LocalDateTime.now());

        OrderComment saved = orderCommentRepository.save(comment);
        return mapComment(saved);
    }

    /**
     * Добавить этап производства.
     */
    public OrderStageResponse addStage(Long orderId, OrderStageRequest stageRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        com.example.orderservice.entity.OrderStage stage = new com.example.orderservice.entity.OrderStage();
        stage.setOrder(order);
        // Workshop lookup would be needed here
        // workshopService.findById(stageRequest.getWorkshopId())
        stage.setWaitPrevious(stageRequest.getWaitPrevious());
        stage.setDueDate(stageRequest.getDueDate());
        stage.setNote(stageRequest.getNote());
        stage.setStatus(stageRequest.getStatus());
        stage.setSourceFiles(stageRequest.getSourceFiles());

        com.example.orderservice.entity.OrderStage saved = orderStageRepository.save(stage);
        return orderMapper.stageToDto(saved);
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
     * Преобразовать сущность Payment в PaymentResponse.
     */
    private PaymentResponse mapPayment(com.example.orderservice.entity.Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getPaymentType(),
                payment.getDetails(),
                payment.getIsPartial()
        );
    }

    /**
     * Преобразовать сущность OrderComment в CommentResponse.
     */
    private CommentResponse mapComment(OrderComment comment) {
        Employee author = comment.getAuthor();
        EmployeeResponse authorDto = author != null ?
                new EmployeeResponse(author.getId(), author.getFullName(), author.getPosition(), author.getPhone(), author.getEmail(), author.getUsername()) :
                null;

        return new CommentResponse(
                comment.getId(),
                comment.getMessage(),
                authorDto,
                comment.getIsInternal(),
                comment.getCreatedAt()
        );
    }

    private OrderMaterialResponse mapOrderMaterial(OrderMaterial om) {
        Material material = om.getMaterial();
        MaterialResponse materialDto = material != null ?
                new MaterialResponse(
                        material.getId(),
                        material.getName(),
                        material.getUnit(),
                        material.getPrice(),
                        material.getWasteCoefficient(),
                        List.of()
                ) : null;

        return new OrderMaterialResponse(
                om.getId(),
                materialDto,
                om.getQuantity(),
                om.getReadyDate(),
                om.getWasteCoefficient(),
                om.getCost()
        );
    }

    /**
     * Добавить позицию (изделие) в заказ.
     * Если указан productId, выполняется динамический расчёт материалов и операций
     * на основе формулы продукта и переданных параметров (width, height, params).
     * Допустимо только для заказов в статусе WAITING.
     */
    @Transactional
    public OrderItemResponse addOrderItem(OrderItemCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Заказ не найден: " + request.getOrderId()));

        // dynamical calculator only for orders in WAITING status
        if (order.getStatus() != OrderStatus.WAITING) {
            throw new IllegalStateException("Добавление позиций возможно только для заказов в статусе 'Ожидание'");
        }

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setName(request.getName() != null ? request.getName() : "Изделие");
        item.setWidth(request.getWidth());
        item.setHeight(request.getHeight());
        item.setQuantity(request.getQuantity());
        if (request.getParams() != null) {
            try {
                String paramsJson = objectMapper.writeValueAsString(request.getParams());
                item.setParams(paramsJson);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка сериализации params", e);
            }
        }
        item.setReadyDate(request.getReadyDate());

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new RuntimeException("Продукт не найден: " + request.getProductId()));
            item.setProduct(product);

            // Динамический расчёт через CalculatorService
            CalculateRequest calcReq = new CalculateRequest(
                    request.getProductId(),
                    request.getWidth(),
                    request.getHeight(),
                    request.getQuantity(),
                    request.getParams()
            );
            CalculationResult calcResult = calculatorService.calculate(calcReq);

            // Установка цены продажи (за единицу) и общей суммы (цена * количество)
            BigDecimal unitSellingPrice = calcResult.getSellingPriceRecommended()
                    .divide(BigDecimal.valueOf(request.getQuantity()), 2, BigDecimal.ROUND_HALF_UP);
            item.setPrice(unitSellingPrice);
            item.setCost(calcResult.getSellingPriceRecommended()); // total revenue

            // Сохраняем позицию, чтобы получить ID
            OrderItem savedItem = orderItemRepository.save(item);

            // Создаём материалы и операции из результата расчёта
            for (ComponentBreakdown comp : calcResult.getBreakdown()) {
                if (comp.getMaterialId() != null) {
                    Material material = entityManager.find(Material.class, comp.getMaterialId());
                    if (material == null) continue;
                    OrderItemMaterial oim = new OrderItemMaterial();
                    oim.setOrderItem(savedItem);
                    oim.setMaterial(material);
                    oim.setQuantity(comp.getQuantity());
                    // Коэффициент отхода берем из ProductMaterial
                    ProductMaterial pm = product.getMaterials().stream()
                            .filter(m -> m.getMaterial().getId().equals(comp.getMaterialId()))
                            .findFirst()
                            .orElse(null);
                    oim.setWasteCoefficient(pm != null ? pm.getWasteCoefficient() : BigDecimal.ONE);
                    oim.setCost(comp.getTotal());
                    savedItem.getMaterials().add(oim);
                } else {
                    // Операция
                    OrderItemOperation oio = new OrderItemOperation();
                    oio.setOrderItem(savedItem);
                    oio.setName(comp.getName());
                    oio.setPricePerUnit(comp.getUnitPrice());
                    oio.setQuantity(comp.getQuantity());
                    oio.setCost(comp.getTotal());
                    savedItem.getOperations().add(oio);
                }
            }

            // Flush to ensure records are visible to JDBC queries
            orderItemRepository.flush();

            // Пересчитываем totals заказа
            recalculateTotalAmount(order.getId());
            recalculateOrderCostAndMargin(order.getId());

            return orderMapper.itemToDto(savedItem);
        } else {
            // Без продукта — просто сохраняем позицию
            OrderItem saved = orderItemRepository.save(item);
            orderItemRepository.flush();
            recalculateTotalAmount(order.getId());
            return orderMapper.itemToDto(saved);
        }
    }

    private void recalculateOrderCostAndMargin(Long orderId) {
        BigDecimal legacyMatCost = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cost),0) FROM order_materials WHERE order_id = ? AND deleted = false",
                new Object[]{orderId}, BigDecimal.class);
        BigDecimal orderMatOpCost = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cost),0) FROM order_material_operations WHERE order_id = ? AND deleted = false",
                new Object[]{orderId}, BigDecimal.class);
        BigDecimal newMatCost = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(om.cost),0) FROM order_item_materials om " +
                "JOIN order_items oi ON om.order_item_id = oi.id " +
                "WHERE oi.order_id = ? AND oi.deleted = false",
                new Object[]{orderId}, BigDecimal.class);
        BigDecimal opCost = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(oo.cost),0) FROM order_item_operations oo " +
                "JOIN order_items oi ON oo.order_item_id = oi.id " +
                "WHERE oi.order_id = ? AND oi.deleted = false",
                new Object[]{orderId}, BigDecimal.class);

        BigDecimal totalCost = legacyMatCost.add(orderMatOpCost).add(newMatCost).add(opCost);
        orderRepository.updateCostPrice(orderId, totalCost);

        // Пересчитываем маржу
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && totalCost.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalAmount = order.getTotalAmount();
            // Если totalAmount ещё не установлен, можно вычислить как totalCost * (1+margin) или наоборот.
            // Здесь просто считаем маржу как (totalAmount - totalCost) / totalCost
            if (totalAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = totalAmount.subtract(totalCost)
                        .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                orderRepository.updateMarginPercent(orderId, margin);
            }
        }
    }
}
