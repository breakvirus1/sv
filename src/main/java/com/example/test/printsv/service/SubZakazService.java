package com.example.test.printsv.service;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.mapper.SubZakazMapper;
import com.example.test.printsv.repository.SubZakazRepository;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.response.SubZakazResponse;

@Service
public class SubZakazService {
    @Autowired
    private SubZakazRepository subZakazRepository;

    @Autowired
    private ZakazRepository zakazRepository;
    @Autowired
    private SubZakazMapper subZakazMapper;

    public SubZakaz addSubZakaz(Long zakazId, SubZakaz subZakaz) {
        Zakaz zakaz = zakazRepository.findById(zakazId)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + zakazId));
        subZakaz.setZakaz(zakaz);
        subZakaz.setCena(subZakaz.getWidth() * subZakaz.getHeight() * subZakaz.getMaterial().getPrice());
        return subZakazRepository.save(subZakaz);
    }

    public List<SubZakazResponse> getAllSubZakazByZakazId(Long zakazId) {
        Zakaz zakaz = zakazRepository.findById(zakazId)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + zakazId));
        List<SubZakaz> subZakazList = subZakazRepository.findAllByZakazId(zakaz.getId());
        return subZakazList.stream()
                .map(subZakazMapper::toSubZakazResponse).collect(Collectors.toList());

        

        
    }

    public SubZakaz updateSubZakaz(Long id, SubZakaz subZakazDetails) {
        SubZakaz subZakaz = subZakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubZakaz not found with ID: " + id));
        subZakaz.setMaterial(subZakazDetails.getMaterial());
        subZakaz.setWidth(subZakazDetails.getWidth());
        subZakaz.setHeight(subZakazDetails.getHeight());
        subZakaz.setCena(subZakazDetails.getCena());
        return subZakazRepository.save(subZakaz);
    }

    public void deleteSubZakaz(Long id) {
        subZakazRepository.deleteById(id);
    }


    public Double setsumForZakaz(Long zakazId) {
        List<SubZakaz> subZakazList = subZakazRepository.findAllByZakazId(zakazId);
        return subZakazList.stream().mapToDouble(SubZakaz::getPrice).sum();
    }
}