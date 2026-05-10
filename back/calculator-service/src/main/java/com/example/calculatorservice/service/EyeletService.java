package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.Eyelet;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.EyeletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EyeletService {

    private final EyeletRepository eyeletRepository;

    public List<Eyelet> getAllEyelets() {
        return eyeletRepository.findAll();
    }

    public Eyelet getEyeletById(Long id) {
        return eyeletRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Люверс не найден"));
    }
}
