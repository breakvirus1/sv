package com.example.test.printsv.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.mapper.ZakazMapper;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ZakazService {

    public ZakazRepository zakazRepository;
    public ZakazMapper zakazMapper;

    @PostConstruct
    public void ZakazInit() {
        log.info("Zakaz initialized successfully.");
    }

    public void createZakaz(ZakazRequest zakazRequest) {
        Zakaz zakaz = zakazMapper.toEntity(zakazRequest);
        zakazRepository.save(zakaz);
    }

    public ZakazResponse getZakazById(Long id) {
        return zakazMapper.fromZakazEntitytoZakazResponse(zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ с id " + id + " не найден")));
    }

    public ZakazResponse updateZakazById(Long id, ZakazRequest zakazRequest) {
        Zakaz existingZakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ с id " + id + " не найден"));

        Zakaz updatedZakaz = zakazMapper.toEntity(zakazRequest);
        updatedZakaz.setId(existingZakaz.getId());

        return zakazMapper.fromZakazEntitytoZakazResponse(zakazRepository.save(updatedZakaz));
    }

    public List<ZakazResponse> getAllZakaz() {
        return zakazRepository.findAll().stream()
                .map(zakazMapper::fromZakazEntitytoZakazResponse)
                .toList();
    }

    public void deleteZakazById(Long id) {
        if (!zakazRepository.existsById(id)) {
            throw new RuntimeException("Заказ с id " + id + " не найден");
        }
        zakazRepository.deleteById(id);
    }

    public List<ZakazResponse> getAllZakazByCustomerName(String customerName){
        
        return zakazRepository.findByCustomerNameIgnoreCase(customerName).stream().toList();
    }

    public void deleteAllZakaz() {
        zakazRepository.deleteAll();
    }


    @PreDestroy
    public void destroyZakaz() {
        log.info("Zakaz destroyed successfully.");
    }

}
