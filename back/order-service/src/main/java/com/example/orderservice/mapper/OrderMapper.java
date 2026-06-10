package com.example.orderservice.mapper;

import com.example.clientservice.dto.ClientResponse;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.materialservice.dto.MaterialResponse;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderMaterialOperation;
import com.example.orderservice.entity.OrderStage;
import com.example.orderservice.order.entity.OrderItemOperation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "updatedAt", source = "updatedAt")
    OrderResponse toDto(Order order);

    @AfterMapping
    default void afterToDto(Order order, @MappingTarget OrderResponse dto) {
        if (order.getClient() != null) {
            dto.setClient(clientToDto(order.getClient()));
        }
        if (order.getManager() != null) {
            dto.setManager(employeeToDto(order.getManager()));
        }
    }

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    Order toEntity(OrderResponse orderResponse);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "width", target = "width")
    @Mapping(source = "height", target = "height")
    @Mapping(source = "params", target = "params")
    @Mapping(source = "operations", target = "operations")
    @Mapping(target = "product", ignore = true)
    OrderItemResponse itemToDto(OrderItem item);

    @Mapping(target = "normTime", expression = "java(op.getNormTime() != null ? op.getNormTime().toString() : null)")
    OrderItemOperationDTO operationToDto(OrderItemOperation op);

    OrderStageResponse stageToDto(OrderStage stage);

    @AfterMapping
    default void afterStageToDto(OrderStage entity, @MappingTarget OrderStageResponse dto) {
        if (entity.getWorkshop() != null) {
            dto.setWorkshop(new WorkshopResponse(
                entity.getWorkshop().getId(),
                entity.getWorkshop().getName(),
                entity.getWorkshop().getSortOrder(),
                entity.getWorkshop().getOperationIds()));
        }
    }

    ClientResponse clientToDto(com.example.clientservice.entity.Client client);

    EmployeeResponse employeeToDto(com.example.employeeservice.entity.Employee employee);

    MaterialResponse materialToDto(com.example.materialservice.entity.Material material);

    default OrderMaterialResponse orderMaterialToDto(OrderMaterial om) {
        OrderMaterialResponse res = new OrderMaterialResponse();
        res.setId(om.getId());
        if (om.getMaterial() != null) {
            res.setMaterialId(om.getMaterial().getId());
            res.setMaterialName(om.getMaterial().getName());
            res.setPricePerUnit(om.getMaterial().getPrice());
            res.setUnit(om.getMaterial().getUnit());
        }
        res.setQuantity(om.getQuantity());
        res.setWasteCoefficient(om.getWasteCoefficient());
        res.setCost(om.getCost());
        res.setReadyDate(om.getReadyDate());
        // operations will be set separately in service to avoid circular mapping
        return res;
    }

    @Mapping(target = "pricePerUnit", source = "basePrice")
    OrderMaterialOperationResponse orderMaterialOperationToDto(OrderMaterialOperation op);

    @AfterMapping
    default void afterOrderMaterialToDto(OrderMaterial entity, @MappingTarget OrderMaterialResponse dto) {
        if (entity.getOrderItem() != null && entity.getOrderItem().getOperations() != null) {
            dto.setOperations(entity.getOrderItem().getOperations().stream()
                .map(op -> new OrderOperationSummary(
                    op.getOperationId(),
                    op.getOperationName(),
                    op.getPricePerUnit(),
                    op.getCalculatedQuantity(),
                    op.getSubtotal(),
                    op.getWidthM(),
                    op.getHeightM()))
                .collect(Collectors.toList()));
        }
        if (entity.getOrderItem() != null) {
            dto.setOrderItemId(entity.getOrderItem().getId());
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "totalAmount", constant = "0")
    @Mapping(target = "paidAmount", constant = "0")
    @Mapping(target = "debtAmount", constant = "0")
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "productionStage", constant = "NOT_STARTED")
    @Mapping(target = "hasDocuments", constant = "false")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    Order toEntity(OrderCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderNumber", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "debtAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "productionStage", ignore = true)
    @Mapping(target = "hasDocuments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromRequest(OrderUpdateRequest request, @MappingTarget Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "paymentDate", expression = "java(request.getPaymentDate() != null ? request.getPaymentDate() : java.time.LocalDate.now())")
    @Mapping(target = "isPartial", expression = "java(request.getIsPartial() != null ? request.getIsPartial() : false)")
    Payment toPaymentEntity(PaymentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "wasteCoefficient", ignore = true)
    @Mapping(target = "cost", ignore = true)
    @Mapping(target = "operations", ignore = true)
    com.example.orderservice.entity.OrderMaterial toOrderMaterialEntity(OrderMaterialCreateRequest request);
}
