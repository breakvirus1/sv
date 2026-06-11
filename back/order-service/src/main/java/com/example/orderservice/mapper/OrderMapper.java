package com.example.orderservice.mapper;

import com.example.clientservice.dto.ClientResponse;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.materialservice.dto.MaterialResponse;
import com.example.orderservice.dto.*;
import com.example.orderservice.entity.*;
import org.mapstruct.AfterMapping;
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

    @Mapping(target = "operations", ignore = true)
    OrderItemResponse itemToDto(OrderItem item);

    @Mapping(target = "operations", ignore = true)
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

    PaymentResponse paymentToDto(Payment payment);

    @Mapping(target = "workshop", ignore = true)
    OrderStageResponse stageToDto(OrderStage stage);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "material", ignore = true)
    @Mapping(target = "wasteCoefficient", ignore = true)
    @Mapping(target = "cost", ignore = true)
    @Mapping(target = "operations", ignore = true)
    OrderMaterial toOrderMaterialEntity(OrderMaterialCreateRequest request);

    ClientResponse clientToDto(com.example.clientservice.entity.Client client);

    EmployeeResponse employeeToDto(com.example.employeeservice.entity.Employee employee);

    MaterialResponse materialToDto(com.example.materialservice.entity.Material material);

    @Mapping(target = "author", ignore = true)
    CommentResponse commentToDto(OrderComment comment);

    @AfterMapping
    default void afterCommentToDto(OrderComment entity, @MappingTarget CommentResponse dto) {
        if (entity.getAuthor() != null) {
            dto.setAuthor(new EmployeeResponse(
                entity.getAuthor().getId(),
                entity.getAuthor().getFullName(),
                entity.getAuthor().getPosition(),
                entity.getAuthor().getPhone(),
                entity.getAuthor().getEmail(),
                entity.getAuthor().getUsername(),
                entity.getAuthor().getWorkshopId()));
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "isInternal", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OrderComment commentToEntity(CommentRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "workshop", ignore = true)
    @Mapping(target = "waitPrevious", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sourceFiles", ignore = true)
    OrderStage stageToEntity(OrderStageRequest request);
}
