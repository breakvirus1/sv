package com.example.clientservice.service;

import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.exception.BadRequestException;
import com.example.clientservice.exception.ConflictException;
import com.example.clientservice.exception.NotFoundException;
import com.example.clientservice.mapper.ClientMapper;
import com.example.clientservice.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ClientMapper clientMapper;

    @Cacheable(value = "clients", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#spec?.toString() ?: 'all') + '_' + (#companyId ?: 'all')")
    public Page<ClientResponse> getAllClients(Specification<Client> spec, Pageable pageable) {
        return getAllClients(spec, pageable, null);
    }

    @Cacheable(value = "clients", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + (#spec?.toString() ?: 'all') + '_' + (#companyId ?: 'all')")
    public Page<ClientResponse> getAllClients(Specification<Client> spec, Pageable pageable, Long companyId) {
        Specification<Client> finalSpec = spec;
        if (companyId != null) {
            finalSpec = spec == null ? Specification.where(null) : spec;
            finalSpec = finalSpec.and((root, query, cb) -> cb.equal(root.get("companyId"), companyId));
        }
        return clientRepository.findAll(finalSpec, pageable)
                .map(clientMapper::toDto);
    }

    @Cacheable(value = "clients", key = "'id_' + #id")
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));
        return clientMapper.toDto(client);
    }

    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse createClient(ClientCreateRequest request) {
        if (clientRepository.findByName(request.getName()).isPresent()) {
            throw new ConflictException("Клиент с таким названием уже существует");
        }
        Client client = clientMapper.createFromRequest(request);
        // Explicitly set priceplus as MapStruct may not map it correctly
        if (request.getPriceplus() != null) {
            client.setPriceplus(request.getPriceplus());
        }
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    @CacheEvict(value = "clients", allEntries = true)
    public ClientResponse updateClient(Long id, ClientUpdateRequest request) {
        Client client = getClientEntity(id);
        clientMapper.updateFromRequest(request, client);
        // Explicitly set priceplus as MapStruct may not map it correctly
        if (request.getPriceplus() != null) {
            client.setPriceplus(request.getPriceplus());
        }
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    @CacheEvict(value = "clients", allEntries = true)
    public void deleteClient(Long id) {
        Client client = getClientEntity(id);
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM svschema.orders WHERE client_id = ? AND deleted = false",
                Integer.class,
                id
        );
        if (orderCount != null && orderCount > 0) {
            throw new BadRequestException("Нельзя удалить клиента с существующими заказами");
        }
        clientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public List<Client> searchClients(String query, Long companyId) {
        Specification<Client> spec = (root, criteriaQuery, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("contactPerson")), "%" + query.toLowerCase() + "%")
                );
        if (companyId != null) {
            spec = spec.and((root, criteriaQuery, cb) -> cb.or(
                    cb.isNull(root.get("companyId")),
                    cb.equal(root.get("companyId"), companyId)));
        }
        return clientRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    private Client getClientEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент не найден"));
    }
}
