package com.example.test.printsv.service;

import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.mapper.ZakazMapper;
import com.example.test.printsv.repository.CustomerRepository;
import com.example.test.printsv.repository.MaterialRepository;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;

import com.example.test.printsv.response.ZakazResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ZakazService {

    private final ZakazRepository zakazRepository;
    private final ZakazMapper zakazMapper;
    private final UserService userService;
    private final CustomerRepository customerRepository;
    private final MaterialRepository materialRepository;
    private final UserRepository userRepository;

    

    @PostConstruct
    public void zakazInit() {
        log.info("ZakazService initialized successfully.");
    }

    @Transactional
    public ZakazResponse createZakaz(Long userId, ZakazRequest zakazRequest) {
        
            Zakaz zakaz = zakazMapper.toZakaz(zakazRequest);
            zakaz.setUserOfZakaz(userService.getUserById(userId));
            Customer customer = customerRepository.findById(zakazRequest.getCustomerOfZakazId())
                    .orElseThrow(() -> new RuntimeException("Клиент с id " + zakazRequest.getCustomerOfZakazId() + " не найден"));
            zakaz.setCustomerOfZakaz(customer);
            zakaz = zakazRepository.save(zakaz);
            return zakazMapper.toZakazResponse(zakaz);
        
    }

    @Transactional(readOnly = true)
    public List<ZakazResponse> getAllZakazByUserName(String userName) {
        Optional<User> user = userRepository.findByUsername(userName);

        if (userService.checkUserAuthorization(user.get().getId())) {
            return zakazRepository.findByUserOfZakaz(user.get()).stream()
                    .map(zakazMapper::toZakazResponse)
                    .collect(Collectors.toList());
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional(readOnly = true)
    public ZakazResponse getZakazById(Long userId, Long id) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + id + " не найден"));
            return zakazMapper.toZakazResponse(zakaz);
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional(readOnly = true)
    public List<ZakazResponse> getAllZakazByCustomerName(String customerName) {

            return zakazRepository.findByCustomerOfZakazIgnoreCase(customerName).stream()
                    .map(zakazMapper::toZakazResponse)
                    .collect(Collectors.toList());
 
    }

    @Transactional
    public ZakazResponse updateZakaz(Long userId, Long id, ZakazRequest zakazRequest) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz existingZakaz = zakazRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + id + " не найден"));
            Zakaz updatedZakaz = zakazMapper.toZakaz(zakazRequest);
            updatedZakaz.setId(existingZakaz.getId());
            updatedZakaz.setUserOfZakaz(existingZakaz.getUserOfZakaz());
            Customer customer = customerRepository.findById(zakazRequest.getCustomerOfZakazId())
                    .orElseThrow(() -> new RuntimeException("Клиент с id " + zakazRequest.getCustomerOfZakazId() + " не найден"));
            updatedZakaz.setCustomerOfZakaz(customer);
            updatedZakaz = zakazRepository.save(updatedZakaz);
            return zakazMapper.toZakazResponse(updatedZakaz);
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional
    public void deleteZakaz(Long userId, Long id) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + id + " не найден"));
            zakazRepository.delete(zakaz);
        } else {
            throw new SecurityException("User ID does not match authenticated user");
        }
    }

    @Transactional
    public SubZakazResponse createSubZakaz(Long userId, Long zakazId, SubZakazRequest subZakazRequest) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(zakazId)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + zakazId + " не найден"));
            SubZakaz subZakaz = new SubZakaz();
            // subZakaz.setMaterialId(materialRepository.findById(subZakazRequest.getName())
            //         .orElseThrow(() -> new RuntimeException("Материал с id " + subZakazRequest.getMaterialId() + " не найден")));
            subZakaz.setLength(subZakazRequest.getLength());
            subZakaz.setWidth(subZakazRequest.getWidth());
            subZakaz.setFilePath(subZakazRequest.getFilePath());
            subZakaz.setComment(subZakazRequest.getComment());
            subZakaz.setCena(subZakazRequest.getCena());
            subZakaz.setDone(subZakazRequest.getDone());
            zakaz.addSubZakaz(subZakaz);
            zakazRepository.save(zakaz);
            return mapToSubZakazResponse(subZakaz);
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional(readOnly = true)
    public List<SubZakazResponse> getAllSubZakazByZakazId(Long userId, Long zakazId) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(zakazId)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + zakazId + " не найден"));
            return zakaz.getSubZakazList().stream()
                    .map(this::mapToSubZakazResponse)
                    .collect(Collectors.toList());
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional(readOnly = true)
    public SubZakazResponse getSubZakazById(Long userId, Long zakazId, Long id) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(zakazId)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + zakazId + " не найден"));
            SubZakaz subZakaz = zakaz.getSubZakazList().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Подзаказ с id " + id + " не найден"));
            return mapToSubZakazResponse(subZakaz);
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional
    public SubZakazResponse updateSubZakaz(Long userId, Long zakazId, Long id, SubZakazRequest subZakazRequest) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(zakazId)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + zakazId + " не найден"));
            SubZakaz subZakaz = zakaz.getSubZakazList().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Подзаказ с id " + id + " не найден"));
            subZakaz.setName(subZakazRequest.getName());
            subZakaz.setLength(subZakazRequest.getLength());
            subZakaz.setWidth(subZakazRequest.getWidth());
            subZakaz.setFilePath(subZakazRequest.getFilePath());
            subZakaz.setComment(subZakazRequest.getComment());
            subZakaz.setCena(subZakazRequest.getCena());
            subZakaz.setDone(subZakazRequest.getDone());
            zakazRepository.save(zakaz);
            return mapToSubZakazResponse(subZakaz);
        }
        throw new SecurityException("User ID does not match authenticated user");
    }

    @Transactional
    public void deleteSubZakaz(Long userId, Long zakazId, Long id) {
        if (userService.checkUserAuthorization(userId)) {
            Zakaz zakaz = zakazRepository.findById(zakazId)
                    .orElseThrow(() -> new RuntimeException("Заказ с id " + zakazId + " не найден"));
            SubZakaz subZakaz = zakaz.getSubZakazList().stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Подзаказ с id " + id + " не найден"));
            zakaz.removeSubZakaz(subZakaz);
            zakazRepository.save(zakaz);
        } else {
            throw new SecurityException("User ID does not match authenticated user");
        }
    }

    @Transactional
    public void deleteAllZakaz() {
        zakazRepository.deleteAll();
    }

    private SubZakazResponse mapToSubZakazResponse(SubZakaz subZakaz) {
        SubZakazResponse response = new SubZakazResponse();
        response.setId(subZakaz.getId());
        response.setName(subZakaz.getName());
        response.setLength(subZakaz.getLength());
        response.setWidth(subZakaz.getWidth());
        response.setFilePath(subZakaz.getFilePath());
        response.setComment(subZakaz.getComment());
        response.setCena(subZakaz.getCena());
        response.setDone(subZakaz.getDone());
        response.setZakazId(subZakaz.getZakaz().getId());
        return response;
    }

    @PreDestroy
    public void destroyZakaz() {
        log.info("ZakazService destroyed successfully.");
    }
}