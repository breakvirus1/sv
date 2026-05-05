package com.example.orderservice.service;

import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.order.entity.OrderItemMaterial;
import com.example.orderservice.order.entity.OrderItemOperation;
import com.example.orderservice.product.Product;
import com.example.orderservice.product.repository.ProductRepository;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.materialservice.entity.Material;
import com.example.orderservice.dto.OrderEstimateDTO;
import com.example.orderservice.dto.OrderItemMaterialDTO;
import com.example.orderservice.dto.OrderItemOperationDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EstimateService {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Расчёт из шаблона продукта (без сохранения)
    public OrderEstimateDTO calculateFromProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        List<OrderItemMaterialDTO> materials = product.getMaterials().stream()
                .map(this::convertToOrderMaterialDto)
                .collect(Collectors.toList());

        List<OrderItemOperationDTO> operations = product.getOperations().stream()
                .map(this::convertToOrderOperationDto)
                .collect(Collectors.toList());

        BigDecimal totalMat = materials.stream()
                .map(m -> m.getCost() != null ? m.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOp = operations.stream()
                .map(op -> op.getCost() != null ? op.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = totalMat.add(totalOp);

        return new OrderEstimateDTO(null, productId, materials, operations, totalMat, totalOp, grandTotal);
    }

    // Сохранение сметы в позицию заказа
    public OrderEstimateDTO saveEstimate(Long orderItemId, OrderEstimateDTO request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException("OrderItem not found"));

        // Обновляем product_id если передан
        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new NotFoundException("Product not found"));
            orderItem.setProduct(product);
        } else {
            orderItem.setProduct(null);
        }

        // Очищаем старые материалы и операции
        orderItem.getMaterials().clear();
        orderItem.getOperations().clear();

        // Добавляем материалы
        for (OrderItemMaterialDTO dto : request.getMaterials()) {
            Material material = entityManager.find(Material.class, dto.getMaterialId());
            if (material == null) {
                throw new NotFoundException("Material not found: " + dto.getMaterialId());
            }
            OrderItemMaterial om = new OrderItemMaterial();
            om.setOrderItem(orderItem);
            om.setMaterial(material);
            om.setQuantity(dto.getQuantity());
            om.setWasteCoefficient(dto.getWasteCoefficient());
            om.setCost(dto.getCost());
            orderItem.getMaterials().add(om);
        }

        // Добавляем операции
        for (OrderItemOperationDTO dto : request.getOperations()) {
            OrderItemOperation op = new OrderItemOperation();
            op.setOrderItem(orderItem);
            op.setName(dto.getName());
            op.setPricePerUnit(dto.getPricePerUnit());
            op.setQuantity(dto.getQuantity());
            op.setCost(dto.getCost());
            // Convert normTime string to Duration if provided
            if (dto.getNormTime() != null && !dto.getNormTime().isEmpty()) {
                try {
                    op.setNormTime(Duration.parse(dto.getNormTime()));
                } catch (Exception e) {
                    // invalid format, ignore or set null
                    op.setNormTime(null);
                }
            }
            orderItem.getOperations().add(op);
        }

        orderItemRepository.save(orderItem);

        return calculateEstimate(orderItemId);
    }

    // Получить текущую смету позиции заказа
    @Transactional(readOnly = true)
    public OrderEstimateDTO calculateEstimate(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new NotFoundException("OrderItem not found"));

        List<OrderItemMaterialDTO> materialDTOs = orderItem.getMaterials().stream()
                .map(this::convertToOrderItemMaterialDto)
                .collect(Collectors.toList());

        List<OrderItemOperationDTO> opDTOs = orderItem.getOperations().stream()
                .map(this::convertToOrderItemOperationDto)
                .collect(Collectors.toList());

        BigDecimal totalMat = materialDTOs.stream()
                .map(OrderItemMaterialDTO::getCost)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOp = opDTOs.stream()
                .map(OrderItemOperationDTO::getCost)
                .filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal grandTotal = totalMat.add(totalOp);

        return new OrderEstimateDTO(
                orderItemId,
                orderItem.getProduct() != null ? orderItem.getProduct().getId() : null,
                materialDTOs, opDTOs, totalMat, totalOp, grandTotal
        );
    }

    // Преобразование ProductMaterial -> DTO
    private OrderItemMaterialDTO convertToOrderMaterialDto(com.example.orderservice.product.ProductMaterial pm) {
        Material material = pm.getMaterial();
        BigDecimal qtyWithWaste = pm.getQuantity().multiply(pm.getWasteCoefficient());
        BigDecimal cost = qtyWithWaste.multiply(material.getPrice());
        return new OrderItemMaterialDTO(
                null,
                material.getId(),
                material.getName(),
                qtyWithWaste,
                pm.getWasteCoefficient(),
                cost,
                material.getPrice(),
                material.getUnit()
        );
    }

    // Преобразование ProductOperation -> DTO
    private OrderItemOperationDTO convertToOrderOperationDto(com.example.orderservice.product.ProductOperation po) {
        BigDecimal cost = po.getPricePerUnit();
        return new OrderItemOperationDTO(
                null,
                po.getName(),
                po.getPricePerUnit(),
                po.getNormTime() != null ? po.getNormTime().toString() : null,
                BigDecimal.ONE,
                cost
        );
    }

    // Преобразование OrderItemMaterial -> DTO
    private OrderItemMaterialDTO convertToOrderItemMaterialDto(OrderItemMaterial om) {
        Material material = om.getMaterial();
        return new OrderItemMaterialDTO(
                om.getId(),
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                om.getQuantity(),
                om.getWasteCoefficient(),
                om.getCost(),
                material != null ? material.getPrice() : null,
                material != null ? material.getUnit() : null
        );
    }

    // Преобразование OrderItemOperation -> DTO
    private OrderItemOperationDTO convertToOrderItemOperationDto(OrderItemOperation oo) {
        return new OrderItemOperationDTO(
                oo.getId(),
                oo.getName(),
                oo.getPricePerUnit(),
                oo.getNormTime() != null ? oo.getNormTime().toString() : null,
                oo.getQuantity(),
                oo.getCost()
        );
    }
}
