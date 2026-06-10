package com.example.orderservice.controller;

import com.example.orderservice.dto.CalculateRequest;
import com.example.orderservice.dto.CalculationResult;
import com.example.orderservice.service.CalculatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculation")
@RequiredArgsConstructor
public class CalculationController {

    private final CalculatorService calculatorService;

    /**
     * Динамический расчет себестоимости и цены по шаблону продукта.
     * Используется для предварительного расчета на фронтенде и при создании заказа.
     *
     * @param request Параметры расчета: productId, width, height, quantity, params
     * @return Результат расчета: materialCost, operationCost, totalCost, sellingPrice, margin, breakdown
     */
    @PostMapping("/calculate")
    public ResponseEntity<CalculationResult> calculate(@Valid @RequestBody CalculateRequest request) {
        CalculationResult result = calculatorService.calculate(request);
        return ResponseEntity.ok(result);
    }
}
