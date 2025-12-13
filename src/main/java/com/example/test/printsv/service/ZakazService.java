
package com.example.test.printsv.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.test.printsv.entity.SubZakaz;
import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.mapper.ZakazMapper;
import com.example.test.printsv.repository.SubZakazRepository;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.request.ZakazRequest;
import com.example.test.printsv.response.ZakazResponse;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Service
@AllArgsConstructor

public class ZakazService {

    private UserRepository userRepository;

    private ZakazRepository zakazRepository;

    private ZakazMapper zakazMapper;
    private SubZakazService subZakazService;

    private SubZakazRepository subZakazRepository;

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public List<ZakazResponse> getAllZakazByUserId(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

        List<Zakaz> zakazList = zakazRepository.findAllByUserId(id);

        return zakazList.stream().map(zakazMapper::toZakazResponse).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ZakazResponse addZakaz(ZakazRequest zakazRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with name: " + username));
        Zakaz zakaz = zakazMapper.toZakaz(zakazRequest);
        zakaz.setUser(user);
        zakaz.setCreatedAt(LocalDateTime.now());
        Zakaz savedZakaz = zakazRepository.save(zakaz);
        subZakazService.setsumForZakaz(zakazRequest.getId());

        return zakazMapper.toZakazResponse(savedZakaz);
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ZakazResponse updateZakaz(Long id) {
        Zakaz zakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + id));
        List<SubZakaz> subZakazList = subZakazRepository.findAllByZakazId(id);
        zakaz.setSubZakazList(subZakazList);

        zakaz.setSum(subZakazService.setsumForZakaz(id));
        Zakaz updatedZakaz = zakazRepository.save(zakaz);

        return zakazMapper.toZakazResponse(updatedZakaz);
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public void deleteZakaz(Long id) {
        zakazRepository.deleteById(id);
    }

    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ZakazResponse getZakazById(Long id) {
        Zakaz zakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + id));

        return zakazMapper.toZakazResponse(zakaz);
    }
}
