package com.example.clientservice.mapper;

import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.entity.ClientType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClientMapper {

    ClientResponse toDto(Client client);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Client toEntity(ClientCreateRequest request);

    default Client createFromRequest(ClientCreateRequest request) {
        Client client = toEntity(request);
        if (request.getType() != null) {
            client.setType(ClientType.valueOf(request.getType()));
        }
        return client;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(ClientUpdateRequest request, @MappingTarget Client client);

    default void updateFromRequest(ClientUpdateRequest request, @MappingTarget Client client) {
        updateEntityFromRequest(request, client);
        if (request.getType() != null) {
            client.setType(ClientType.valueOf(request.getType()));
        }
    }
}
