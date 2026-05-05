package com.example.orderservice.mapper;

import com.example.clientservice.dto.ClientResponse;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.materialservice.dto.MaterialResponse;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderMaterial;
import com.example.orderservice.entity.OrderStage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * MapStruct mapper для преобразования между сущностями и DTO.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(source = "client.id", target = "client.id")
    @Mapping(source = "client.name", target = "client.name")
    @Mapping(source = "client.type", target = "client.type")
    @Mapping(source = "client.contactPerson", target = "client.contactPerson")
    @Mapping(source = "client.phone", target = "client.phone")
    @Mapping(source = "client.email", target = "client.email")
    @Mapping(source = "manager.id", target = "manager.id")
    @Mapping(source = "manager.fullName", target = "manager.fullName")
    @Mapping(source = "manager.position", target = "manager.position")
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    OrderResponse toDto(Order order);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    Order toEntity(OrderResponse orderResponse);

    @Mapping(source = "product.id", target = "productId")
    OrderItemResponse itemToDto(OrderItem item);

    OrderStageResponse stageToDto(OrderStage stage);

    ClientResponse clientToDto(com.example.clientservice.entity.Client client);

    EmployeeResponse employeeToDto(com.example.employeeservice.entity.Employee employee);

    MaterialResponse materialToDto(com.example.materialservice.entity.Material material);

    OrderMaterialResponse orderMaterialToDto(OrderMaterial orderMaterial);

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
    @Mapping(target = "status", constant = "WAITING")
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
    com.example.orderservice.entity.Payment toPaymentEntity(PaymentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "wasteCoefficient", ignore = true)
    @Mapping(target = "cost", ignore = true)
    com.example.orderservice.entity.OrderMaterial toOrderMaterialEntity(OrderMaterialCreateRequest request);
}
