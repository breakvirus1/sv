package com.example.clientservice.service;

import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.mapper.ClientMapper;
import com.example.clientservice.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
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

    public Page<ClientResponse> getAllClients(Specification<Client> spec, Pageable pageable) {
        return clientRepository.findAll(spec, pageable)
                .map(clientMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        return clientMapper.toDto(client);
    }

    public ClientResponse createClient(ClientCreateRequest request) {
        if (clientRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Клиент с таким названием уже существует");
        }
        Client client = clientMapper.createFromRequest(request);
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    public ClientResponse updateClient(Long id, ClientUpdateRequest request) {
        Client client = getClientEntity(id);
        clientMapper.updateFromRequest(request, client);
        Client saved = clientRepository.save(client);
        return clientMapper.toDto(saved);
    }

    public void deleteClient(Long id) {
        Client client = getClientEntity(id);
        Integer orderCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM orders WHERE client_id = ? AND deleted = false",
            Integer.class,
            id
        );
        if (orderCount != null && orderCount > 0) {
            throw new RuntimeException("Нельзя удалить клиента с существующими заказами");
        }
        clientRepository.delete(client);
    }

    @Transactional(readOnly = true)
    public List<Client> searchClients(String query) {
        return clientRepository.findAll((root, queryBuilder, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + query.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("contactPerson")), "%" + query.toLowerCase() + "%")
                ));
    }

    @Transactional(readOnly = true)
    private Client getClientEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }
}
