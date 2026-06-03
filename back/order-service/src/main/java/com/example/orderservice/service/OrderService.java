package com.example.orderservice.service;

import com.example.clientservice.entity.Client;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.entity.Employee;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.entity.Material;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.FileAttachment;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderComment;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderOperation;
import com.example.orderservice.entity.OrderStage;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ProductionStage;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.FileAttachmentRepository;
import com.example.orderservice.repository.OrderCommentRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OrderStageRepository;
import com.example.orderservice.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.orderservice.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Set;
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
    private final FileAttachmentRepository fileAttachmentRepository;
    private final OrderMapper orderMapper;
    @PersistenceContext
    private EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${calculator.service.url}")
    private String calculatorUrl;

    /**
     * Получить список заказов с фильтрацией и пагинацией.
     */
    @Transactional(readOnly = true)
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
                 .orElseThrow(() -> new NotFoundException("Заказ не найден"));
         return mapOrderResponse(order);
     }

    /**
     * Получить заказ по его номеру (orderNumber).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            throw new NotFoundException("Заказ не найден");
        }
        return mapOrderResponse(order);
    }

    private OrderResponse mapOrderResponse(Order order) {
        OrderResponse response = orderMapper.toDto(order);

        response.setItems(order.getItems().stream()
                .map(item -> {
                    OrderItemResponse dto = orderMapper.itemToDto(item);
                    fileAttachmentRepository.findByOrderItemId(item.getId()).ifPresent(f -> {
                        dto.setFileUrl(f.getFileUrl());
                        dto.setFileOriginalName(f.getOriginalName());
                    });
                    return dto;
                })
                .collect(Collectors.toList()));

        response.setStages(order.getStages().stream()
                .map(orderMapper::stageToDto)
                .collect(Collectors.toList()));

        response.setPayments(order.getPayments().stream()
                .map(orderMapper::paymentToDto)
                .collect(Collectors.toList()));

        response.setComments(order.getComments().stream()
                .map(orderMapper::commentToDto)
                .collect(Collectors.toList()));

        List<OrderMaterial> allMaterials = order.getItems().stream()
                .flatMap(item -> item.getMaterials().stream())
                .collect(Collectors.toList());
        response.setMaterials(allMaterials.stream()
                .map(orderMapper::orderMaterialToDto)
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
        order.setPriceplus(request.getPriceplus());
        // Use provided total amount if available, otherwise calculate from items
        BigDecimal providedTotal = request.getTotalAmount() != null ? request.getTotalAmount() : BigDecimal.ZERO;
        order.setTotalAmount(providedTotal);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setDebtAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.DRAFT);
        order.setProductionStage(ProductionStage.NOT_STARTED);

        Order saved = orderRepository.save(order);

        // Обработка позиций заказа
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // Get current user's JWT token to propagate to calculator service
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String jwtToken = null;
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                jwtToken = ((Jwt) auth.getPrincipal()).getTokenValue();
            }

            BigDecimal total = BigDecimal.ZERO;
            for (OrderMaterialCreateRequest itemReq : request.getItems()) {
                Material material = entityManager.find(Material.class, itemReq.getMaterialId());
                if (material == null) {
                    throw new RuntimeException("Материал не найден: " + itemReq.getMaterialId());
                }

                 // Build request to calculator service
                 Map<String, Object> calcRequest = new HashMap<>();
                 calcRequest.put("materialId", material.getId());
                 calcRequest.put("widthM", itemReq.getWidthM());
                 calcRequest.put("heightM", itemReq.getHeightM());
                 List<Long> opIds = itemReq.getOperations() == null ? Collections.emptyList() :
                     itemReq.getOperations().stream()
                         .map(OrderOperationRequest::getOperationId)
                         .collect(Collectors.toList());
                 calcRequest.put("operationIds", opIds);

                  // Include eyelet parameters if present
                  if (itemReq.getEyeletId() != null) {
                      calcRequest.put("eyeletId", itemReq.getEyeletId());
                  }
                  if (itemReq.getEyeletStepCm() != null) {
                      calcRequest.put("eyeletStepCm", itemReq.getEyeletStepCm());
                  }

                  // Include podvorot parameters if present
                  if (itemReq.getPodvorotMmHorizontal() != null) {
                      calcRequest.put("podvorotMmHorizontal", itemReq.getPodvorotMmHorizontal());
                  }
                  if (itemReq.getPodvorotMmVertical() != null) {
                      calcRequest.put("podvorotMmVertical", itemReq.getPodvorotMmVertical());
                  }
                  if (itemReq.getPodvorotCountPerSide() != null) {
                      calcRequest.put("podvorotCountPerSide", itemReq.getPodvorotCountPerSide());
                  }

                // Prepare headers with JWT
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (jwtToken != null) {
                    headers.setBearerAuth(jwtToken);
                }
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(calcRequest, headers);

                // Call calculator service
                Map<String, Object> calcResponse;
                try {
                    ResponseEntity<Map> response = restTemplate.exchange(
                        calculatorUrl + "/api/v1/calculations/preview",
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                    );
                    calcResponse = response.getBody();
                } catch (RestClientException e) {
                    throw new RuntimeException("Ошибка при расчете стоимости позиции: " + e.getMessage(), e);
                }

                if (calcResponse == null) {
                    throw new RuntimeException("Пустой ответ от службы расчета");
                }

// Parse total price
                Number totalPriceNum = (Number) calcResponse.get("totalPrice");
                BigDecimal totalPrice = new BigDecimal(totalPriceNum.toString());

                // Parse operations breakdown
                List<Map<String, Object>> opsData = (List<Map<String, Object>>) calcResponse.get("operations");
                BigDecimal operationsSubtotal = BigDecimal.ZERO;
                List<OrderOperation> orderOps = new ArrayList<>();

                // Build a map from operationId -> request operation for width/height lookup
                Map<Long, OrderOperationRequest> reqOpMap = new HashMap<>();
                if (itemReq.getOperations() != null) {
                    for (OrderOperationRequest reqOp : itemReq.getOperations()) {
                        reqOpMap.put(reqOp.getOperationId(), reqOp);
                    }
                }

                if (opsData != null) {
                    for (Map<String, Object> opMap : opsData) {
                        Long opId = ((Number) opMap.get("operationId")).longValue();
                        String opName = (String) opMap.get("operationName");
                        Number qtyNum = (Number) opMap.get("quantity");
                        Number priceNum = (Number) opMap.get("pricePerUnit");
                        Number subtotalNum = (Number) opMap.get("subtotal");
                        BigDecimal qty = new BigDecimal(qtyNum.toString());
                        BigDecimal pricePerUnit = new BigDecimal(priceNum.toString());
                        BigDecimal subtotal = new BigDecimal(subtotalNum.toString());
                        operationsSubtotal = operationsSubtotal.add(subtotal);

                        OrderOperation orderOp = new OrderOperation();
                        orderOp.setOperationId(opId);
                        orderOp.setOperationName(opName);
                        orderOp.setPricePerUnit(pricePerUnit);
                        orderOp.setCalculatedQuantity(qty);
                        orderOp.setSubtotal(subtotal);

                        // Attach width/height from request if provided
                        OrderOperationRequest reqOp = reqOpMap.get(opId);
                        if (reqOp != null) {
                            orderOp.setWidthM(reqOp.getWidthM());
                            orderOp.setHeightM(reqOp.getHeightM());
                        }

                        orderOps.add(orderOp);
                    }
                }

                // Compute material cost from known formula to avoid double-counting eyelet hardware
                BigDecimal wasteCoeff = material.getWasteCoefficient();
                if (wasteCoeff == null) wasteCoeff = BigDecimal.ONE;
                BigDecimal materialPrice = material.getPrice();
                BigDecimal height = itemReq.getHeightM() != null ? itemReq.getHeightM() : BigDecimal.ONE;
                BigDecimal materialArea = itemReq.getWidthM().multiply(height);
                BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff).setScale(2, RoundingMode.HALF_UP);

                // Effective area = materialCost / (price * wasteCoeff) = materialArea
                BigDecimal effectiveArea = materialArea.setScale(2, RoundingMode.HALF_UP);

                // Compute eyelet hardware cost from calculator response (totalPrice includes it)
                BigDecimal eyeletCost = totalPrice.subtract(operationsSubtotal).subtract(materialCost);

                // Calculate cost with priceplus
                BigDecimal priceplus = saved.getPriceplus() != null ? saved.getPriceplus() : BigDecimal.ZERO;
                BigDecimal costPriceplus = totalPrice.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

                // Create OrderItem
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(saved);
                orderItem.setName(material.getName() + " " + itemReq.getWidthM() + "x" + itemReq.getHeightM() + "m");
                orderItem.setQuantity(1);
                orderItem.setReadyDate(itemReq.getReadyDate());
                orderItem.setPrice(totalPrice);
                orderItem.setCost(totalPrice);

                // Create OrderMaterial
                OrderMaterial orderMaterial = new OrderMaterial();
                orderMaterial.setOrder(order);
                orderMaterial.setOrderItem(orderItem);
                orderMaterial.setMaterial(material);
                orderMaterial.setQuantity(effectiveArea);
                orderMaterial.setWasteCoefficient(wasteCoeff);
                orderMaterial.setCost(materialCost);
                orderMaterial.setCostPriceplus(costPriceplus);
                orderMaterial.setEyeletCost(eyeletCost.compareTo(BigDecimal.ZERO) > 0 ? eyeletCost : BigDecimal.ZERO);
                orderMaterial.setWidthM(itemReq.getWidthM());
                orderMaterial.setHeightM(itemReq.getHeightM());
                order.getMaterials().add(orderMaterial);
                orderItem.getMaterials().add(orderMaterial);

                // Attach operations
                for (OrderOperation op : orderOps) {
                    op.setOrderItem(orderItem);
                    orderItem.getOperations().add(op);
                }

                saved.getItems().add(orderItem);
                // Only add to total if we need to calculate it (not provided)
                if (request.getTotalAmount() == null) {
                    total = total.add(totalPrice);
                }
            }
            // Only update total if we calculated it ourselves
            if (request.getTotalAmount() == null) {
                saved.setTotalAmount(total);
            }
            // Calculate totalWithPriceplus
            BigDecimal priceplus = saved.getPriceplus() != null ? saved.getPriceplus() : BigDecimal.ZERO;
            BigDecimal totalWithPriceplus = total.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
            saved.setTotalWithPriceplus(totalWithPriceplus.setScale(2, RoundingMode.HALF_UP));

            // === ВАЛИДАЦИЯ РАСЧЕТОВ из calculation service (с минимальной погрешностью) ===
            BigDecimal clientCalc = request.getClientTotalWithPriceplus() != null ? request.getClientTotalWithPriceplus() : request.getTotalAmount();
            if (clientCalc != null) {
                BigDecimal diff = totalWithPriceplus.subtract(clientCalc).abs();
                BigDecimal tolerance = new BigDecimal("0.01");
                if (diff.compareTo(tolerance) > 0) {
                    throw new RuntimeException("Валидация расчетов не пройдена: расхождение фронтенда с calculation service = " + diff + " (допуск " + tolerance + "). Обновите форму.");
                }
                // Лог успешной валидации
                System.out.println("=== ORDER VALIDATION PASSED ===");
                System.out.println("Order ID: " + saved.getId());
                System.out.println("Order number: " + saved.getOrderNumber());
                System.out.println("Frontend total: " + clientCalc);
                System.out.println("Backend total: " + totalWithPriceplus);
                System.out.println("Difference: " + diff);
                System.out.println("==============================");
            }

            orderRepository.save(saved);
        }

        return getOrderById(saved.getId());
    }

    private static final AtomicInteger sequence = new AtomicInteger(0);

    /**
     * Сгенерировать уникальный номер заказа.
     * Формат: YYYYMMDDHHmmssSSS (где SSS - последовательный номер для уникальности)
     */
    private String generateOrderNumber() {
        while (true) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            int seq = sequence.getAndIncrement() % 1000;
            String orderNumber = timestamp + String.format("%03d", seq);
            if (orderRepository.findByOrderNumber(orderNumber) == null) {
                return orderNumber;
            }
        }
    }

    /**
     * Обновить статус заказа.
     * При смене статуса могут выполняться дополнительные действия (уведомления, триггеры).
     */
    public OrderResponse updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

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
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        order.setProductionStage(ProductionStage.valueOf(stage));
        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }

    /**
     * Обновить заказ.
     */
    @Transactional
    public OrderResponse updateOrder(Long id, OrderUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        if (request.getDescription() != null) {
            order.setDescription(request.getDescription());
        }
        if (request.getOrderDate() != null) {
            order.setOrderDate(request.getOrderDate());
        }
        if (request.getDueDate() != null) {
            order.setDueDate(request.getDueDate());
        }

        if (request.getManagerId() != null) {
            Employee manager = entityManager.find(Employee.class, request.getManagerId());
            if (manager == null) {
                throw new RuntimeException("Менеджер не найден");
            }
            order.setManager(manager);
        }

if (request.getPriceplus() != null) {
             order.setPriceplus(request.getPriceplus());
         }

        List<OrderMaterialCreateRequest> itemRequests = request.getItems();
        if (itemRequests != null) {
            List<OrderMaterial> existingMaterials = new ArrayList<>(order.getMaterials());
            Map<Long, OrderMaterial> existingById = existingMaterials.stream()
                    .collect(Collectors.toMap(com.example.orderservice.entity.BaseEntity::getId, m -> m));

            // Get JWT token for calculator service
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String jwtToken = null;
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                jwtToken = ((Jwt) auth.getPrincipal()).getTokenValue();
            }

            // Collect old operations for cleanup
            List<OrderOperation> oldOps = new ArrayList<>();
            for (OrderItem item : order.getItems()) {
                oldOps.addAll(item.getOperations());
            }

            BigDecimal total = BigDecimal.ZERO;

            for (OrderMaterialCreateRequest itemReq : itemRequests) {
                Material material = entityManager.find(Material.class, itemReq.getMaterialId());
                if (material == null) {
                    throw new RuntimeException("Материал не найден: " + itemReq.getMaterialId());
                }

                BigDecimal widthM = itemReq.getWidthM();
                BigDecimal heightM = itemReq.getHeightM();

                // Build calculator request
                Map<String, Object> calcRequest = new HashMap<>();
                calcRequest.put("materialId", material.getId());
                calcRequest.put("widthM", widthM);
                calcRequest.put("heightM", heightM);
                List<Long> opIds = itemReq.getOperations() == null ? Collections.emptyList() :
                    itemReq.getOperations().stream()
                        .map(OrderOperationRequest::getOperationId)
                        .collect(Collectors.toList());
                calcRequest.put("operationIds", opIds);

                // Include eyelet parameters
                if (itemReq.getEyeletId() != null) {
                    calcRequest.put("eyeletId", itemReq.getEyeletId());
                }
                if (itemReq.getEyeletStepCm() != null) {
                    calcRequest.put("eyeletStepCm", itemReq.getEyeletStepCm());
                }

                // Include podvorot parameters
                if (itemReq.getPodvorotMmHorizontal() != null) {
                    calcRequest.put("podvorotMmHorizontal", itemReq.getPodvorotMmHorizontal());
                }
                if (itemReq.getPodvorotMmVertical() != null) {
                    calcRequest.put("podvorotMmVertical", itemReq.getPodvorotMmVertical());
                }
                if (itemReq.getPodvorotCountPerSide() != null) {
                    calcRequest.put("podvorotCountPerSide", itemReq.getPodvorotCountPerSide());
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (jwtToken != null) {
                    headers.setBearerAuth(jwtToken);
                }
                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(calcRequest, headers);

                Map<String, Object> calcResponse;
                try {
                    ResponseEntity<Map> response = restTemplate.exchange(
                        calculatorUrl + "/api/v1/calculations/preview",
                        HttpMethod.POST,
                        requestEntity,
                        Map.class
                    );
                    calcResponse = response.getBody();
                } catch (RestClientException e) {
                    throw new RuntimeException("Ошибка при расчете стоимости позиции: " + e.getMessage(), e);
                }

                if (calcResponse == null) {
                    throw new RuntimeException("Пустой ответ от службы расчета");
                }

                Number totalPriceNum = (Number) calcResponse.get("totalPrice");
                BigDecimal totalPrice = new BigDecimal(totalPriceNum.toString());

                // Parse operations breakdown
                List<Map<String, Object>> opsData = (List<Map<String, Object>>) calcResponse.get("operations");
                BigDecimal operationsSubtotal = BigDecimal.ZERO;
                List<OrderOperation> orderOps = new ArrayList<>();

                // Build map from operationId -> request operation for width/height lookup
                Map<Long, OrderOperationRequest> reqOpMap = new HashMap<>();
                if (itemReq.getOperations() != null) {
                    for (OrderOperationRequest reqOp : itemReq.getOperations()) {
                        reqOpMap.put(reqOp.getOperationId(), reqOp);
                    }
                }

                if (opsData != null) {
                    for (Map<String, Object> opMap : opsData) {
                        Long opId = ((Number) opMap.get("operationId")).longValue();
                        String opName = (String) opMap.get("operationName");
                        Number qtyNum = (Number) opMap.get("quantity");
                        Number priceNum = (Number) opMap.get("pricePerUnit");
                        Number subtotalNum = (Number) opMap.get("subtotal");
                        BigDecimal qty = new BigDecimal(qtyNum.toString());
                        BigDecimal pricePerUnit = new BigDecimal(priceNum.toString());
                        BigDecimal subtotal = new BigDecimal(subtotalNum.toString());
                        operationsSubtotal = operationsSubtotal.add(subtotal);

                        OrderOperation orderOp = new OrderOperation();
                        orderOp.setOperationId(opId);
                        orderOp.setOperationName(opName);
                        orderOp.setPricePerUnit(pricePerUnit);
                        orderOp.setCalculatedQuantity(qty);
                        orderOp.setSubtotal(subtotal);

                        OrderOperationRequest reqOp = reqOpMap.get(opId);
                        if (reqOp != null) {
                            orderOp.setWidthM(reqOp.getWidthM());
                            orderOp.setHeightM(reqOp.getHeightM());
                        }

                        orderOps.add(orderOp);
                    }
                }

                // Compute material cost from known formula to avoid double-counting eyelet hardware
                BigDecimal wasteCoeff = material.getWasteCoefficient();
                if (wasteCoeff == null) wasteCoeff = BigDecimal.ONE;
                BigDecimal materialPrice = material.getPrice();
                BigDecimal h = heightM != null ? heightM : BigDecimal.ONE;
                BigDecimal materialArea = widthM.multiply(h);
                BigDecimal materialCost = materialArea.multiply(materialPrice).multiply(wasteCoeff).setScale(2, RoundingMode.HALF_UP);

                // Effective area = materialCost / (price * wasteCoeff) = materialArea
                BigDecimal effectiveArea = materialArea.setScale(2, RoundingMode.HALF_UP);

                // Calculate cost with priceplus
                BigDecimal priceplus = order.getPriceplus() != null ? order.getPriceplus() : BigDecimal.ZERO;
                BigDecimal costPriceplus = totalPrice.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));

                if (itemReq.getId() != null) {
                    OrderMaterial om = existingById.get(itemReq.getId());
                    if (om != null) {
                        om.setMaterial(material);
                        om.setReadyDate(itemReq.getReadyDate());
                        om.setWidthM(widthM);
                        om.setHeightM(heightM);
                        om.setQuantity(effectiveArea);
                        om.setWasteCoefficient(wasteCoeff);
                        om.setCost(materialCost);
                        om.setCostPriceplus(costPriceplus);

                        // Update operations for the linked order item
                        OrderItem orderItem = om.getOrderItem();
                        if (orderItem != null) {
                            orderItem.getOperations().clear();
                            for (OrderOperation op : orderOps) {
                                op.setOrderItem(orderItem);
                                orderItem.getOperations().add(op);
                            }
                            orderItem.setPrice(totalPrice);
                            orderItem.setCost(totalPrice);
                        }
                    }
                } else {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setOrder(order);
                    orderItem.setName(material.getName() + " " + widthM + "x" + heightM + "m");
                    orderItem.setQuantity(1);
                    orderItem.setReadyDate(itemReq.getReadyDate());
                    orderItem.setPrice(totalPrice);
                    orderItem.setCost(totalPrice);

                    for (OrderOperation op : orderOps) {
                        op.setOrderItem(orderItem);
                        orderItem.getOperations().add(op);
                    }

                    OrderMaterial om = new OrderMaterial();
                    om.setOrder(order);
                    om.setOrderItem(orderItem);
                    om.setMaterial(material);
                    om.setQuantity(effectiveArea);
                    om.setWasteCoefficient(wasteCoeff);
                    om.setCost(materialCost);
                    om.setCostPriceplus(costPriceplus);
                    om.setWidthM(widthM);
                    om.setHeightM(heightM);
                    om.setReadyDate(itemReq.getReadyDate());
                    order.getMaterials().add(om);
                    orderItem.getMaterials().add(om);

                    order.getItems().add(orderItem);
                }

                total = total.add(totalPrice);
            }

            // Remove materials no longer in the submitted list
            Set<Long> updatedIds = itemRequests.stream()
                    .filter(r -> r.getId() != null)
                    .map(OrderMaterialCreateRequest::getId)
                    .collect(Collectors.toSet());
            existingMaterials.stream()
                    .filter(m -> !updatedIds.contains(m.getId()))
                    .forEach(order.getMaterials()::remove);

            // Use provided totalAmount or calculate from items
            if (request.getTotalAmount() != null) {
                order.setTotalAmount(request.getTotalAmount());
            } else {
                order.setTotalAmount(total);
            }
 // Calculate totalWithPriceplus
              BigDecimal priceplus = order.getPriceplus() != null ? order.getPriceplus() : BigDecimal.ZERO;
              BigDecimal totalWithPriceplus = total.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
              order.setTotalWithPriceplus(totalWithPriceplus.setScale(2, RoundingMode.HALF_UP));

              // === ВАЛИДАЦИЯ РАСЧЕТОВ из calculation service (минимальная погрешность) ===
              BigDecimal clientCalc = request.getClientTotalWithPriceplus() != null ? request.getClientTotalWithPriceplus() : request.getTotalAmount();
              if (clientCalc != null) {
                  BigDecimal diff = totalWithPriceplus.subtract(clientCalc).abs();
                  BigDecimal tolerance = new BigDecimal("0.01");
                  if (diff.compareTo(tolerance) > 0) {
                      throw new RuntimeException("Валидация расчетов не пройдена: расхождение фронтенда с calculation service = " + diff + " (допуск " + tolerance + ").");
                  }
                  // Лог успешной валидации
                  System.out.println("=== ORDER UPDATE VALIDATION PASSED ===");
                  System.out.println("Order ID: " + order.getId());
                  System.out.println("Order number: " + order.getOrderNumber());
                  System.out.println("Frontend total: " + clientCalc);
                  System.out.println("Backend total: " + totalWithPriceplus);
                  System.out.println("Difference: " + diff);
                  System.out.println("=====================================");
              }

              orderRepository.save(order);

         }

OrderResponse response = mapOrderResponse(order);
         return response;
     }

    /**
     * Добавить оплату к заказу.
     * Автоматически пересчитывает paidAmount и debtAmount.
     */
    public PaymentResponse addPayment(Long orderId, PaymentRequest paymentRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        com.example.orderservice.entity.Payment payment = orderMapper.toPaymentEntity(paymentRequest);
        payment.setOrder(order);

        Payment saved = paymentRepository.save(payment);

        // Пересчитаем оплаченную сумму и долг
        recalculatePaidAmount(orderId);
        return orderMapper.paymentToDto(saved);
    }

    /**
     * Добавить комментарий к заказу.
     */
    public CommentResponse addComment(Long orderId, CommentRequest commentRequest, EmployeeResponse author) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        OrderComment comment = orderMapper.commentToEntity(commentRequest);
        comment.setOrder(order);
        if (commentRequest.getIsInternal() != null) {
            comment.setIsInternal(commentRequest.getIsInternal());
        }
        if (author != null) {
            Employee authorEntity = entityManager.find(Employee.class, author.getId());
            comment.setAuthor(authorEntity);
        }
        comment.setCreatedAt(LocalDateTime.now());

        OrderComment saved = orderCommentRepository.save(comment);
        return orderMapper.commentToDto(saved);
    }

    /**
     * Добавить этап производства.
     */
    public OrderStageResponse addStage(Long orderId, OrderStageRequest stageRequest) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        com.example.orderservice.entity.OrderStage stage = orderMapper.stageToEntity(stageRequest);
        stage.setOrder(order);

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
        BigDecimal paid = paymentRepository.sumByOrderId(orderId);
        orderRepository.updatePaidAmount(orderId, paid);
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
        MaterialResponse materialDto = om.getMaterial() != null ?
                orderMapper.materialToDto(om.getMaterial()) :
                null;

        // Populate operations from the linked order item (if any)
        List<OrderOperationSummary> opSummaries = List.of();
        OrderItem orderItem = om.getOrderItem();
        if (orderItem != null && orderItem.getOperations() != null) {
            opSummaries = orderItem.getOperations().stream()
                .map(op -> new OrderOperationSummary(
                    op.getOperationId(),
                    op.getOperationName(),
                    op.getPricePerUnit(),
                    op.getCalculatedQuantity(),
                    op.getSubtotal(),
                    op.getWidthM(),
                    op.getHeightM()))
                .collect(Collectors.toList());
        }

        return new OrderMaterialResponse(
                om.getId(),
                materialDto,
                om.getQuantity(),
                om.getWidthM(),
                om.getHeightM(),
                om.getReadyDate(),
                om.getWasteCoefficient(),
                om.getCost(),
                om.getCostPriceplus(),
                om.getEyeletCost(),
                opSummaries
        );
    }

    /**
     * Получить информацию о существующей позиции заказа (материал в заказе).
     * Подтягивает ширину и высоту: если в заказе не заданы, берёт defaultWidthMm/defaultHeightMm из справочника.
     * Используется фронтендом при редактировании линии заказа для подтягивания размеров из БД.
     */
    public ItemPositionInfo getItemPositionInfo(Long orderId, Long orderMaterialId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));

        OrderMaterial om = order.getMaterials().stream()
                .filter(m -> m.getId().equals(orderMaterialId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Позиция в заказе не найдена: " + orderMaterialId));

        BigDecimal widthM = om.getWidthM() != null ? om.getWidthM() : om.getMaterial().getDefaultWidthM();
        BigDecimal heightM = om.getHeightM() != null ? om.getHeightM() : om.getMaterial().getDefaultHeightM();

        MaterialResponse matResp = orderMapper.materialToDto(om.getMaterial());

        return new ItemPositionInfo(om.getId(), matResp, widthM, heightM);
    }

    /**
     * Рассчитать общую стоимость заказа без сохранения в БД.
     * Используется фронтендом для отображения суммы в реальном времени.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOpenOrderTotal(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));
        return order.getMaterials().stream()
                .map(om -> {
                    Material mat = om.getMaterial();
                    BigDecimal price = mat.getPrice() != null ? mat.getPrice() : BigDecimal.ZERO;
                    BigDecimal waste  = mat.getWasteCoefficient() != null ? mat.getWasteCoefficient() : BigDecimal.ONE;

                    BigDecimal q1 = om.getWidthM()  != null ? om.getWidthM()  : mat.getDefaultWidthM();
                    BigDecimal q2 = om.getHeightM() != null ? om.getHeightM() : mat.getDefaultHeightM();
                    q1 = q1 != null ? q1 : BigDecimal.ZERO;
                    q2 = q2 != null ? q2 : BigDecimal.ZERO;

                    BigDecimal effectiveQty;
                    if ("м2".equals(mat.getUnit())) {
                        effectiveQty = q1.multiply(q2);
                    } else if ("м.п.".equals(mat.getUnit())) {
                        effectiveQty = q1;
                    } else {
                        effectiveQty = q1;
                    }
                    return price.multiply(effectiveQty).multiply(waste);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

/**
      * Получить расчитанные позиции заказа с учетом priceplus.
      * Используется фронтендом для отображения расчетов в реальном времени.
      */
@Transactional(readOnly = true)
      public CalculatedOrderResponse getCalculatedOrder(Long orderId) {
          Order order = orderRepository.findById(orderId)
                  .orElseThrow(() -> new NotFoundException("Заказ не найден"));

          // Use order's priceplus, or fallback to client's default priceplus
          BigDecimal priceplus = order.getPriceplus() != null ? order.getPriceplus() : 
                  (order.getClient() != null && order.getClient().getPriceplus() != null ? order.getClient().getPriceplus() : BigDecimal.ZERO);

// Total without priceplus is the sum of cost + eyeletCost (base cost before priceplus)
          BigDecimal totalWithoutPriceplus = order.getMaterials().stream()
                  .map(om -> {
                      BigDecimal cost = om.getCost() != null ? om.getCost() : BigDecimal.ZERO;
                      BigDecimal eyelet = om.getEyeletCost() != null ? om.getEyeletCost() : BigDecimal.ZERO;
                      return cost.add(eyelet);
                  })
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

// Total with priceplus
          BigDecimal totalWithPriceplus = order.getMaterials().stream()
                  .map(om -> {
                      BigDecimal cost = om.getCost() != null ? om.getCost() : BigDecimal.ZERO;
                      BigDecimal eyelet = om.getEyeletCost() != null ? om.getEyeletCost() : BigDecimal.ZERO;
                      return cost.add(eyelet).multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
                  })
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

          // Also sum operations cost
          BigDecimal totalOperationCost = order.getItems().stream()
                  .flatMap(item -> item.getOperations().stream())
                  .map(op -> op.getSubtotal() != null ? op.getSubtotal() : BigDecimal.ZERO)
                  .reduce(BigDecimal.ZERO, BigDecimal::add);

          totalWithoutPriceplus = totalWithoutPriceplus.add(totalOperationCost);
          totalWithPriceplus = totalWithPriceplus.add(
                  totalOperationCost.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
          );

List<MaterialCalculation> materials = order.getMaterials().stream()
                   .map(om -> {
                       BigDecimal cost = om.getCost() != null ? om.getCost() : BigDecimal.ZERO;
                       BigDecimal eyelet = om.getEyeletCost() != null ? om.getEyeletCost() : BigDecimal.ZERO;
                       BigDecimal base = cost.add(eyelet);
                       BigDecimal costPriceplus = base.multiply(BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
                       String fileUrl = null;
                       String fileOriginalName = null;
                       if (om.getOrderItem() != null) {
                           FileAttachment file = fileAttachmentRepository.findByOrderItemId(om.getOrderItem().getId()).orElse(null);
                           if (file != null) {
                               fileUrl = file.getFileUrl();
                               fileOriginalName = file.getOriginalName();
                           }
                       }
                       return new MaterialCalculation(
                               om.getId(),
                               om.getMaterial() != null ? om.getMaterial().getId() : null,
                               om.getMaterial() != null ? om.getMaterial().getName() : "—",
                               om.getWidthM(),
                               om.getHeightM(),
                               om.getCost(),
                               costPriceplus,
                               fileUrl,
                               fileOriginalName
                       );
                   })
                   .collect(Collectors.toList());

return new CalculatedOrderResponse(
                  orderId,
                  priceplus,
                  totalWithoutPriceplus.setScale(2, RoundingMode.HALF_UP),
                  totalWithPriceplus.setScale(2, RoundingMode.HALF_UP),
                  materials
          );
      }

}
