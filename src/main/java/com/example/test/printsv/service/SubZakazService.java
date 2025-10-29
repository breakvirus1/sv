package com.example.test.printsv.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.repository.SubZakazRepository;
import com.example.test.printsv.repository.ZakazRepository;

@Service
public class SubZakazService {
    @Autowired
    private SubZakazRepository subZakazRepository;

    @Autowired
    private ZakazRepository zakazRepository;

    public SubZakaz addSubZakaz(Long zakazId, SubZakaz subZakaz) {
        Zakaz zakaz = zakazRepository.findById(zakazId)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + zakazId));
        subZakaz.setZakaz(zakaz);
        return subZakazRepository.save(subZakaz);
    }

    public List<SubZakaz> getAllSubZakazByZakazId(Long zakazId) {
        return subZakazRepository.findAllByZakazId(zakazId);
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
}