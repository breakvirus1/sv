package com.example.clientservice.service;

import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.entity.ClientType;
import com.example.clientservice.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для управления клиентами.
 * Предоставляет CRUD операции, а также поиск по имени и контактному лицу.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Получить список клиентов с пагинацией и фильтрацией.
     */
    public Page<ClientResponse> getAllClients(Specification<Client> spec, Pageable pageable) {
        return clientRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Получить клиента по ID.
     */
    @Transactional(readOnly = true)
    public ClientResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
        return mapToResponse(client);
    }

    /**
     * Создать нового клиента.
     * Проверяет уникальность имени.
     */
    public ClientResponse createClient(ClientCreateRequest request) {
        if (clientRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Клиент с таким названием уже существует");
        }
        Client client = new Client();
        client.setName(request.getName());
        client.setType(ClientType.valueOf(request.getType()));
        client.setContactPerson(request.getContactPerson());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setInn(request.getInn());
        client.setAddress(request.getAddress());

        Client saved = clientRepository.save(client);
        return mapToResponse(saved);
    }

    /**
     * Обновить данные клиента.
     */
    public ClientResponse updateClient(Long id, ClientUpdateRequest request) {
        Client client = getClientEntity(id);
        client.setName(request.getName());
        client.setType(ClientType.valueOf(request.getType()));
        client.setContactPerson(request.getContactPerson());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setInn(request.getInn());
        client.setAddress(request.getAddress());
        Client saved = clientRepository.save(client);
        return mapToResponse(saved);
    }

    /**
     * Удалить клиента (если у него нет заказов).
     */
    public void deleteClient(Long id) {
        Client client = getClientEntity(id);
        // Проверяем, есть ли у клиента заказы через прямую проверку в БД
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

    /**
     * Поиск клиентов по запросу (по имени или контактному лицу).
     */
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

    @Transactional(readOnly = true)
    private ClientResponse mapToResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getType() != null ? client.getType().name() : null,
                client.getContactPerson(),
                client.getPhone(),
                client.getEmail(),
                client.getInn(),
                client.getAddress()
        );
    }
}
