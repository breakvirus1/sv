package com.example.clientservice.service;

import com.example.common.entity.Client;
import com.example.clientservice.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    /**
     * Получить список клиентов с пагинацией и фильтрацией.
     */
    public Page<Client> getAllClients(Specification<Client> spec, Pageable pageable) {
        return clientRepository.findAll(spec, pageable);
    }

    /**
     * Получить клиента по ID.
     */
    @Transactional(readOnly = true)
    public Client getClientById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Клиент не найден"));
    }

    /**
     * Создать нового клиента.
     * Проверяет уникальность имени.
     */
    public Client createClient(Client client) {
        if (clientRepository.findByName(client.getName()).isPresent()) {
            throw new RuntimeException("Клиент с таким названием уже существует");
        }
        return clientRepository.save(client);
    }

    /**
     * Обновить данные клиента.
     */
    public Client updateClient(Long id, Client clientDetails) {
        Client client = getClientById(id);
        client.setName(clientDetails.getName());
        client.setType(clientDetails.getType());
        client.setContactPerson(clientDetails.getContactPerson());
        client.setPhone(clientDetails.getPhone());
        client.setEmail(clientDetails.getEmail());
        client.setInn(clientDetails.getInn());
        client.setAddress(clientDetails.getAddress());
        return clientRepository.save(client);
    }

    /**
     * Удалить клиента (если у него нет заказов).
     */
    public void deleteClient(Long id) {
        Client client = getClientById(id);
        // Проверяем, есть ли у клиента заказы
        if (client.getOrders() != null && !client.getOrders().isEmpty()) {
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
}
