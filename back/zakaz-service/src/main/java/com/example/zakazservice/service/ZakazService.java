package com.example.zakazservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common.dto.ZakazDto;
import com.example.common.entity.User;
import com.example.common.entity.Zakaz;
import com.example.zakazservice.repository.UserRepository;
import com.example.zakazservice.mapper.ZakazMapper;
import com.example.zakazservice.repository.SubZakazRepository;
import com.example.zakazservice.repository.ZakazRepository;
import com.example.zakazservice.response.ZakazResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ZakazService {

    private final UserRepository userRepository;
    private final ZakazRepository zakazRepository;
    private final ZakazMapper zakazMapper;
    private final SubZakazService subZakazService;
    private final SubZakazRepository subZakazRepository;

    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional(readOnly = true)
    public List<ZakazResponse> getAllZakazByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        List<Zakaz> zakazList = zakazRepository.findAllByUserId(userId);
        return zakazList.stream()
                .map(zakazMapper::toZakazResponse)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional
    public ZakazResponse addZakaz(ZakazDto zakazDto) {
        User user = userRepository.findById(zakazDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + zakazDto.getUserId()));

        Zakaz zakaz = zakazMapper.toEntity(zakazDto);
        zakaz.setUser(user);
        zakaz.setCreatedAt(LocalDateTime.now());
        zakaz.setSum(0.0);

        Zakaz savedZakaz = zakazRepository.save(zakaz);

        if (zakazDto.getSubZakazList() != null) {
            for (var subDto : zakazDto.getSubZakazList()) {
                subZakazService.addSubZakaz(savedZakaz.getId(), subDto);
            }
        }

        // Recalculate sum
        Double total = subZakazService.calculateZakazSum(savedZakaz.getId());
        savedZakaz.setSum(total);
        zakazRepository.save(savedZakaz);

        // Fetch the zakaz with its subZakazList to correctly map to response
        Zakaz fullZakaz = zakazRepository.findById(savedZakaz.getId())
                .orElseThrow(() -> new RuntimeException("Zakaz not found after creation"));
        return zakazMapper.toZakazResponse(fullZakaz);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional(readOnly = true)
    public ZakazResponse getZakazById(Long id) {
        Zakaz zakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + id));
        return zakazMapper.toZakazResponse(zakaz);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    @Transactional
    public ZakazResponse updateZakaz(Long id) {
        Zakaz zakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + id));
        // Recalculate sum
        Double total = subZakazService.calculateZakazSum(id);
        zakaz.setSum(total);
        Zakaz updatedZakaz = zakazRepository.save(zakaz);
        return zakazMapper.toZakazResponse(updatedZakaz);
    }

    @PreAuthorize("hasRole('OPERATOR')")
    public void deleteZakaz(Long id) {
        zakazRepository.deleteById(id);
    }
}
