
package com.example.test.printsv.service;


























































import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.test.printsv.entity.User;
import com.example.test.printsv.entity.Zakaz;
import com.example.test.printsv.mapper.ZakazMapper;
import com.example.test.printsv.repository.UserRepository;
import com.example.test.printsv.repository.ZakazRepository;
import com.example.test.printsv.response.ListZakazByUserIdResponse;
import com.example.test.printsv.response.ZakazResponse;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Service
@AllArgsConstructor

public class ZakazService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ZakazRepository zakazRepository;
    @Autowired
    private ZakazMapper zakazMapper;


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ListZakazByUserIdResponse getAllZakazByUserId(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
        
        List<Zakaz> zakazList = zakazRepository.findAllByUserId(id);


        List<ZakazResponse> zakazResponses = zakazList.stream()
                .map(zakazMapper::toZakazResponse)
                .collect(Collectors.toList());
    ListZakazByUserIdResponse listZakazByUserIdResponse = new ListZakazByUserIdResponse();
    listZakazByUserIdResponse.setId(user.getId());
    listZakazByUserIdResponse.setUsername(user.getUsername());
    listZakazByUserIdResponse.setZakazList(zakazResponses);

        return listZakazByUserIdResponse;
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ZakazResponse addZakaz(Integer sum) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with name: " + username));
        Zakaz zakaz = new Zakaz();
        zakaz.setUser(user);
        zakaz.setCreatedAt(LocalDateTime.now());
        zakaz.setSum(sum);
        Zakaz savedZakaz = zakazRepository.save(zakaz);

        return zakazMapper.toZakazResponse(savedZakaz);
    }


    @PreAuthorize("hasRole('ROLE_OPERATOR')")
    public ZakazResponse updateZakaz(Long id, Integer sum) {
        Zakaz zakaz = zakazRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Zakaz not found with ID: " + id));
        zakaz.setSum(sum);
        zakaz.setCreatedAt(LocalDateTime.now());
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

