package com.example.test.printsv.service;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.mapper.SubZakazMapper;
import com.example.test.printsv.repository.SubZakazRepository;
import com.example.test.printsv.request.SubZakazRequest;
import com.example.test.printsv.response.SubZakazResponse;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubZakazService {

    


    @PostConstruct
    public void initSubZakaz() {
        log.info("SubZakaz initialized successfully.");
    }

    private SubZakazRepository subZakazRepository;
    private SubZakazMapper subZakazMapper;

    public SubZakaz createZakaz(SubZakazRequest subZakazRequest) {
        SubZakaz zakaz = subZakazMapper.toEntity(subZakazRequest);
        return subZakazRepository.save(zakaz);
    }

    public List<SubZakazResponse> getAllSubZakaz() {
        return subZakazRepository.findAll().stream().map(subZakazMapper::toSubzakazResponse).toList();
    }

    public SubZakazResponse getSubZakazById(Long id) {
        return subZakazMapper.toSubzakazResponse(
                subZakazRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Позиция с " + id + " не найдена")));
    }

    public SubZakaz updateSubZakaz(Long id, SubZakazRequest updatedSubZakazRequest) {
        SubZakaz exSubZakaz = subZakazRepository.findById(id)
                .orElseThrow(()->new RuntimeException(String.format("позиция с %d не найдена", id)));
        SubZakaz updatedSubZakaz = subZakazMapper.toEntity(updatedSubZakazRequest);
        updatedSubZakaz.setId(exSubZakaz.getId());

        return subZakazRepository.save(updatedSubZakaz);
    }


    public void deleteSubZakaz(Long id) {
        if (subZakazRepository.existsById(id)){
                subZakazRepository.deleteById(id);
        }
    }

    public void deleteAllSubZakaz(){
        if (subZakazRepository.count()>0){
            subZakazRepository.deleteAll();
        }
    }

    @PreDestroy
    public void destroySubZakaz() {
        log.info("SubZakaz destroyed successfully.");
    }
}