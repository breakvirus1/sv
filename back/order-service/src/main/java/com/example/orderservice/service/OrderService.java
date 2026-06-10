package com.example.orderservice.service;

import com.example.clientservice.entity.Client;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.entity.Employee;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialOperation;
import com.example.materialservice.entity.OperationType;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.FileAttachment;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderComment;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderMaterialOperation;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.ProductionStage;
import com.example.orderservice.entity.Workshop;
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
    private final FileAttachmentRepository fileAttachmentRepository;
    private final WorkshopRepository workshopRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final CalculatorService calculatorService;
    private final ObjectMapper objectMapper;

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
        spec = spec.and(workshopFilterForCurrentUser());
        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toDto);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream()
                .filter(a -> a.getAuthority().startsWith("USERNAME_"))
                .map(a -> a.getAuthority().substring("USERNAME_".length()))
                .findFirst()
                .orElse(null);
    }

    private boolean isProductionUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PRODUCTION"));
    }

    private List<Long> getWorkshopOperationIds(String username) {
        Employee employee = (Employee) entityManager.createQuery(
                "SELECT e FROM Employee e WHERE e.username = :username AND e.deleted = false")
                .setParameter("username", username)
                .getResultStream().findFirst().orElse(null);
        if (employee == null || employee.getWorkshopId() == null) return null;
        Workshop workshop = workshopRepository.findById(employee.getWorkshopId()).orElse(null);
        if (workshop == null || workshop.getOperationIds() == null || workshop.getOperationIds().isEmpty()) return null;
        return workshop.getOperationIds();
    }

    /**
     * Build a specification that filters orders by the current PRODUCTION user's workshop operations.
     * For ADMIN/MANAGER/ACCOUNTANT roles, no workshop filtering is applied.
     * For PRODUCTION, returns orders where any OrderItem has an OrderOperation with operationId
     * matching one of the workshop's operationIds.
     */
    private Specification<Order> workshopFilterForCurrentUser() {
        if (!isProductionUser()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String username = getCurrentUsername();
        if (username == null) {
            return (root, query, cb) -> cb.disjunction();
        }

        List<Long> operationIds = getWorkshopOperationIds(username);
        if (operationIds == null) {
            return (root, query, cb) -> cb.disjunction();
        }

        return (root, query, cb) -> {
            jakarta.persistence.criteria.Join<Order, OrderItem> itemJoin = root.join("items", jakarta.persistence.criteria.JoinType.INNER);
            jakarta.persistence.criteria.Join<OrderItem, OrderOperation> opJoin = itemJoin.join("operations", jakarta.persistence.criteria.JoinType.INNER);
            return opJoin.get("operationId").in(operationIds);
        };
    }

     /**
      * Получить детальную информацию о заказе со всеми связанными сущностями.
      */
    @Transactional(readOnly = true)
public OrderResponse getOrderById(Long id) {
         Order order = orderRepository.findById(id)
                 .orElseThrow(() -> new NotFoundException("Заказ не найден"));
         checkWorkshopAccess(order);
         return mapOrderResponse(order);
     }

    private void checkWorkshopAccess(Order order) {
        if (!isProductionUser()) return;

        String username = getCurrentUsername();
        if (username == null) return;

        List<Long> operationIds = getWorkshopOperationIds(username);
        if (operationIds == null) {
            throw new NotFoundException("Заказ не найден");
        }

        boolean hasMatchingOperation = order.getItems().stream()
                .flatMap(item -> item.getOperations().stream())
                .anyMatch(op -> operationIds.contains(op.getOperationId()));

        if (!hasMatchingOperation) {
            throw new NotFoundException("Заказ не найден");
        }
    }

    /**
     * Получить заказ по его номеру (orderNumber).
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithAllDetails(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        OrderResponse response = orderMapper.toDto(order);

        // Items with operations are mapped automatically via itemToDto (operations included)
        response.setItems(order.getItems().stream()
                .map(item -> {
                    OrderItemResponse dto = orderMapper.itemToDto(item);
                    fileAttachmentRepository.findByOrderItemId(item.getId()).ifPresent(f -> {
                        dto.setFileUrl(f.getFileUrl());
                    });
                    return dto;
                })
                .collect(Collectors.toList()));

        // Stages
        response.setStages(order.getStages().stream()
                .map(orderMapper::stageToDto)
                .collect(Collectors.toList()));

        // Payments
        response.setPayments(order.getPayments().stream()
                .map(orderMapper::paymentToDto)
                .collect(Collectors.toList()));

        // Comments
        response.setComments(order.getComments().stream()
                .map(orderMapper::commentToDto)
                .collect(Collectors.toList()));

        // Materials with operations
        response.setMaterials(order.getMaterials().stream()
                .map(mat -> {
                    OrderMaterialResponse matRes = orderMapper.orderMaterialToDto(mat);
                    matRes.setOperations(mat.getOperations().stream()
                            .map(orderMapper::orderMaterialOperationToDto)
                            .collect(Collectors.toList()));
                    return matRes;
                })
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
                        BigDecimal operationQty;
                        if (template != null) {
                            BigDecimal width = matReq.getWidth();
                            BigDecimal height = matReq.getHeight();
                            Integer itemCnt = matReq.getItemCount();
                            Map<String, Object> params = opReq.getParameters();
                            switch (template.getOperationType()) {
                                case EYELETS:
                                    if (width == null || height == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        CalculationHelper helper = new CalculationHelper(width, height);
                                        Number step = null, edge = null;
                                        if (params != null) {
                                            Object s = params.get("step");
                                            Object e = params.get("edgeDistance");
                                            if (s instanceof Number) step = (Number) s;
                                            if (e instanceof Number) edge = (Number) e;
                                        }
                                        BigDecimal baseQty = helper.eyeletCount(step, edge, itemCnt);
                                        BigDecimal wasteCoef = opReq.getWasteCoefficient() != null ? opReq.getWasteCoefficient() : template.getWasteCoefficient();
                                        operationQty = baseQty.multiply(wasteCoef);
                                    }
                                    break;
                                case CUTTING:
                                    if (width == null || height == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        Number marginW = null, marginH = null, sidesNum = null;
                                        if (params != null) {
                                            if (params.get("marginWidth") instanceof Number) marginW = (Number) params.get("marginWidth");
                                            if (params.get("marginHeight") instanceof Number) marginH = (Number) params.get("marginHeight");
                                            if (params.get("sides") instanceof Number) sidesNum = (Number) params.get("sides");
                                        }
                                        int mw = marginW != null ? marginW.intValue() : 50;
                                        int mh = marginH != null ? marginH.intValue() : 50;
                                        int sides = sidesNum != null ? sidesNum.intValue() : 1;
                                        BigDecimal widthMm = width.multiply(BigDecimal.valueOf(1000));
                                        BigDecimal heightMm = height.multiply(BigDecimal.valueOf(1000));
                                        BigDecimal extraAreaMm2 = BigDecimal.valueOf(mw).multiply(widthMm).multiply(BigDecimal.valueOf(sides))
                                                .add(BigDecimal.valueOf(mh).multiply(heightMm).multiply(BigDecimal.valueOf(sides)));
                                        int cnt = itemCnt != null ? itemCnt : 1;
                                        operationQty = extraAreaMm2.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(cnt));
                                    }
                                    break;
                                case PRINT:
                                    if (width == null || height == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        BigDecimal areaPerItem = width.multiply(height);
                                        operationQty = areaPerItem.multiply(BigDecimal.valueOf(itemCnt != null ? itemCnt : 1));
                                    }
                                    break;
                                case LAMINATION:
                                    if (width == null || height == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        Number lamMarginW = null, lamMarginH = null;
                                        if (params != null) {
                                            if (params.get("marginWidth") instanceof Number) lamMarginW = (Number) params.get("marginWidth");
                                            if (params.get("marginHeight") instanceof Number) lamMarginH = (Number) params.get("marginHeight");
                                        }
                                        double marginWm = (lamMarginW != null ? lamMarginW.doubleValue() : 20) / 1000.0;
                                        double marginHm = (lamMarginH != null ? lamMarginH.doubleValue() : 20) / 1000.0;
                                        BigDecimal effW = width.add(BigDecimal.valueOf(2 * marginWm));
                                        BigDecimal effH = height.add(BigDecimal.valueOf(2 * marginHm));
                                        BigDecimal areaLam = effW.multiply(effH);
                                        operationQty = areaLam.multiply(BigDecimal.valueOf(itemCnt != null ? itemCnt : 1));
                                    }
                                    break;
                                case WELDING:
                                    if (width == null || height == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        BigDecimal perimeter = width.add(height).multiply(BigDecimal.valueOf(2));
                                        operationQty = perimeter.multiply(BigDecimal.valueOf(itemCnt != null ? itemCnt : 1));
                                    }
                                    break;
                                default:
                                    operationQty = opReq.getQuantity() != null ? opReq.getQuantity() : BigDecimal.ONE;
                                    break;
                            }
                        } else {
                            operationQty = opReq.getQuantity() != null ? opReq.getQuantity() : BigDecimal.ONE;
                        }
                                    BigDecimal widthEye = matReq.getWidth();
                                    BigDecimal heightEye = matReq.getHeight();
                                    if (widthEye == null || heightEye == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        CalculationHelper helper = new CalculationHelper(widthEye, heightEye);
                                        BigDecimal baseQty = helper.eyeletCount(step, edge, matReq.getItemCount());
                                        BigDecimal wasteCoef = opReq.getWasteCoefficient() != null ? opReq.getWasteCoefficient() : template.getWasteCoefficient();
                                        operationQty = baseQty.multiply(wasteCoef);
                                    }
                                    break;
                                case CUTTING:
                                    Number marginWNum = null;
                                    Number marginHNum = null;
                                    Number sidesNum = null;
                                    Map<String, Object> paramsCut = opReq.getParameters();
                                    if (paramsCut != null) {
                                        if (paramsCut.get("marginWidth") instanceof Number) marginWNum = (Number) paramsCut.get("marginWidth");
                                        if (paramsCut.get("marginHeight") instanceof Number) marginHNum = (Number) paramsCut.get("marginHeight");
                                        if (paramsCut.get("sides") instanceof Number) sidesNum = (Number) paramsCut.get("sides");
                                    }
                                    int marginW = marginWNum != null ? marginWNum.intValue() : 50;
                                    int marginH = marginHNum != null ? marginHNum.intValue() : 50;
                                    int sides = sidesNum != null ? sidesNum.intValue() : 1;
                                    BigDecimal widthCut = matReq.getWidth();
                                    BigDecimal heightCut = matReq.getHeight();
                                    if (widthCut == null || heightCut == null) {
                                        operationQty = BigDecimal.ONE;
                                    } else {
                                        // Convert to mm
                                        BigDecimal widthMm = widthCut.multiply(BigDecimal.valueOf(1000));
                                        BigDecimal heightMm = heightCut.multiply(BigDecimal.valueOf(1000));
                                        BigDecimal extraAreaMm2 = BigDecimal.valueOf(marginW).multiply(widthMm).multiply(BigDecimal.valueOf(sides))
                                                .add(BigDecimal.valueOf(marginH).multiply(heightMm).multiply(BigDecimal.valueOf(sides)));
                                        int itemCount = matReq.getItemCount() != null ? matReq.getItemCount() : 1;
                                        operationQty = extraAreaMm2.divide(BigDecimal.valueOf(1_000_000), 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(itemCount));
                                    }
                                    break;
                                default:
                                    operationQty = opReq.getQuantity() != null ? opReq.getQuantity() : BigDecimal.ONE;
                                    break;
                            }
                        } else {
                            operationQty = opReq.getQuantity() != null ? opReq.getQuantity() : BigDecimal.ONE;
                        }
                        orderOp.setQuantity(operationQty);
                        BigDecimal opCost = orderOp.getBasePrice().multiply(operationQty).multiply(orderOp.getWasteCoefficient());
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

        if (stageRequest.getWorkshopId() != null) {
            Workshop workshop = entityManager.find(Workshop.class, stageRequest.getWorkshopId());
            if (workshop == null) {
                throw new NotFoundException("Цех не найден: " + stageRequest.getWorkshopId());
            }
            stage.setWorkshop(workshop);
        }

        com.example.orderservice.entity.OrderStage saved = orderStageRepository.save(stage);
        orderRepository.save(order);
        return orderMapper.stageToDto(saved);
    }

    /**
     * Пересчитать общую сумму заказа на основе позиций.
     */
    void recalculateTotalAmount(Long orderId) {
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
                new EmployeeResponse(author.getId(), author.getFullName(), author.getPosition(), author.getPhone(), author.getEmail(), author.getUsername(), author.getWorkshopId()) :
                null;

        return new CommentResponse(
                comment.getId(),
                comment.getMessage(),
                authorDto,
                comment.getIsInternal(),
                comment.getCreatedAt()
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

    void recalculateOrderCostAndMargin(Long orderId) {
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
                        List<OrderOperationSummary> operations = List.of();
                        BigDecimal operationsTotal = BigDecimal.ZERO;
                        BigDecimal operationsTotalPriceplus = BigDecimal.ZERO;
                        if (om.getOrderItem() != null) {
                            FileAttachment file = fileAttachmentRepository.findByOrderItemId(om.getOrderItem().getId()).orElse(null);
                            if (file != null) {
                                fileUrl = file.getFileUrl();
                            }
                           if (om.getOrderItem().getOperations() != null) {
                               operations = om.getOrderItem().getOperations().stream()
                                   .map(op -> new OrderOperationSummary(
                                       op.getOperationId(),
                                       op.getOperationName(),
                                       op.getPricePerUnit(),
                                       op.getCalculatedQuantity(),
                                       op.getSubtotal(),
                                       op.getWidthM(),
                                       op.getHeightM()))
                                   .collect(Collectors.toList());
                               operationsTotal = operations.stream()
                                   .map(op -> op.getSubtotal() != null ? op.getSubtotal() : BigDecimal.ZERO)
                                   .reduce(BigDecimal.ZERO, BigDecimal::add);
                               operationsTotalPriceplus = operationsTotal.multiply(
                                   BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)));
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
                               operationsTotal,
                               operationsTotalPriceplus,
                                fileUrl,
                                operations
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
