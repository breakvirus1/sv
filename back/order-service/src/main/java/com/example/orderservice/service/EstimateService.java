package com.example.orderservice.service;

import com.example.orderservice.dto.OrderEstimateDTO;
import com.example.orderservice.dto.OrderItemMaterialDTO;
import com.example.orderservice.dto.OrderItemOperationDTO;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderOperation;
import com.example.orderservice.repository.OrderItemRepository;
import com.example.materialservice.entity.Material;
import com.example.materialservice.repository.MaterialRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EstimateService {

    private final OrderItemRepository orderItemRepository;
    private final MaterialRepository materialRepository;
    private final OrderService orderService;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderEstimateDTO saveEstimate(Long orderItemId, OrderEstimateDTO request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

        orderItem.getMaterials().clear();
        orderItem.getOperations().clear();

        for (OrderItemMaterialDTO dto : request.getMaterials()) {
            Material material = entityManager.find(Material.class, dto.getMaterialId());
            if (material == null) {
                throw new RuntimeException("Material not found: " + dto.getMaterialId());
            }
            OrderMaterial om = new OrderMaterial();
            om.setOrderItem(orderItem);
            om.setMaterial(material);
            om.setQuantity(dto.getQuantity());
            om.setWasteCoefficient(dto.getWasteCoefficient());
            om.setCost(dto.getCost());
            orderItem.getMaterials().add(om);
        }

        for (OrderItemOperationDTO dto : request.getOperations()) {
            OrderOperation op = new OrderOperation();
            op.setOrderItem(orderItem);
            op.setOperationName(dto.getName());
            op.setPricePerUnit(dto.getPricePerUnit());
            op.setCalculatedQuantity(dto.getQuantity());
            op.setSubtotal(dto.getCost());
            orderItem.getOperations().add(op);
        }

        orderItemRepository.save(orderItem);
        orderItemRepository.flush();

        BigDecimal totalMat = orderItem.getMaterials().stream()
                .map(OrderMaterial::getCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOp = orderItem.getOperations().stream()
                .map(OrderOperation::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandTotal = totalMat.add(totalOp);
        orderItem.setCost(grandTotal);

        orderItemRepository.flush();

        Long orderId = orderItem.getOrder().getId();
        orderService.recalculateTotalAmount(orderId);

        return calculateEstimate(orderItemId);
    }

    @Transactional(readOnly = true)
    public OrderEstimateDTO calculateEstimate(Long orderItemId) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("OrderItem not found"));

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
                null,
                materialDTOs, opDTOs, totalMat, totalOp, grandTotal
        );
    }

    private OrderItemMaterialDTO convertToOrderItemMaterialDto(OrderMaterial om) {
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

    private OrderItemOperationDTO convertToOrderItemOperationDto(OrderOperation oo) {
        return new OrderItemOperationDTO(
                oo.getId(),
                oo.getOperationName(),
                oo.getPricePerUnit(),
                null,
                oo.getCalculatedQuantity(),
                oo.getSubtotal()
        );
    }
}
