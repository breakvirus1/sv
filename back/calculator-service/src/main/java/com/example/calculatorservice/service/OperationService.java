package com.example.calculatorservice.service;

import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationService {

    private final OperationRepository operationRepository;

    public List<Operation> getAllOperations() {
        return operationRepository.findAll();
    }

     public Operation getOperationById(Long id) {
         return operationRepository.findById(id)
                 .orElseThrow(() -> new ResourceNotFoundException("Операция не найдена"));
     }

     public Operation save(Operation operation) {
         return operationRepository.save(operation);
     }

     public void deleteOperation(Long id) {
         Operation operation = getOperationById(id);
         operationRepository.delete(operation);
     }
 }
