package com.example.orderservice.mapper;

import com.example.common.dto.*;
import com.example.common.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
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
    OrderDto toDto(Order order);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "stages", ignore = true)
    @Mapping(target = "payments", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "materials", ignore = true)
    Order toEntity(OrderDto orderDto);

    OrderItemDto itemToDto(OrderItem item);

    OrderStageDto stageToDto(OrderStage stage);

    ClientDto clientToDto(Client client);

    EmployeeDto employeeToDto(Employee employee);

    MaterialDto materialToDto(Material material);

    OrderMaterialDto orderMaterialToDto(OrderMaterial orderMaterial);
}
