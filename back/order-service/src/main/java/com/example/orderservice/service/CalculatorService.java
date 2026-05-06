package com.example.orderservice.service;

import com.example.orderservice.dto.CalculateRequest;
import com.example.orderservice.dto.CalculationResult;
import com.example.orderservice.dto.ComponentBreakdown;
import com.example.orderservice.product.Product;
import com.example.orderservice.product.ProductMaterial;
import com.example.orderservice.product.ProductOperation;
import com.example.orderservice.product.repository.ProductRepository;
import com.example.materialservice.entity.Material;
import com.example.materialservice.repository.MaterialRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.commons.jexl3.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalculatorService {

    private final ProductRepository productRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Динамический расчет себестоимости изделия по шаблону продукта.
     * Использует JEXL для вычисления количеств по формулам.
     */
    public CalculationResult calculate(CalculateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + request.getProductId()));

        // JEXL engine для вычисления формул
        JexlEngine jexl = new JexlBuilder().create();
        JexlContext context = new MapContext();

        // Базовые переменные
        context.set("width", request.getWidth());
        context.set("height", request.getHeight());
        context.set("quantity", request.getQuantity());

        // Дополнительные параметры из формы (цвет, толщина и т.д.)
        if (request.getParams() != null) {
            for (Map.Entry<String, Object> entry : request.getParams().entrySet()) {
                context.set(entry.getKey(), entry.getValue());
            }
        }

        // Устанавливаем параметры по умолчанию для расчёта люверсов, если они не переданы
        if (context.get("step") == null) {
            context.set("step", 300);
        }
        if (context.get("edgeDistance") == null) {
            context.set("edgeDistance", 15);
        }

        // Добавляем вспомогательный объект для кастомных расчётов (например, люверсы)
        context.set("helper", new CalculationHelper());

        List<ComponentBreakdown> breakdown = new ArrayList<>();
        BigDecimal materialTotal = BigDecimal.ZERO;
        BigDecimal operationTotal = BigDecimal.ZERO;

        // Расчёт материалов
        for (ProductMaterial pm : product.getMaterials()) {
            Material material = pm.getMaterial();
            if (material == null) continue;

            // Вычисляем количество по формуле (или используем базовое quantity)
            BigDecimal qtyWithWaste = evaluateQuantity(pm.getQuantityFormula(), context, pm.getWasteCoefficient(), pm.getQuantity());

            // Стоимость = (количество с отходами) * цена материала
            BigDecimal cost = qtyWithWaste.multiply(material.getPrice());

            materialTotal = materialTotal.add(cost);
            breakdown.add(new ComponentBreakdown(
                    material.getName(),
                    material.getId(),
                    qtyWithWaste,
                    material.getPrice(),
                    cost
            ));
        }

        // Расчёт операций
        for (ProductOperation po : product.getOperations()) {
            BigDecimal operationQty;

            // Если у операции задана формула количества, вычисляем её
            if (po.getQuantityFormula() != null && !po.getQuantityFormula().trim().isEmpty()) {
                operationQty = evaluateQuantity(po.getQuantityFormula(), context, BigDecimal.ONE, BigDecimal.valueOf(request.getQuantity()));
            } else {
                // По умолчанию количество операции = количество изделий
                operationQty = BigDecimal.valueOf(request.getQuantity());
            }

            BigDecimal cost = po.getPricePerUnit().multiply(operationQty);
            operationTotal = operationTotal.add(cost);
            breakdown.add(new ComponentBreakdown(
                    po.getName(),
                    null, // materialId null for operations
                    operationQty,
                    po.getPricePerUnit(),
                    cost
            ));
        }

        BigDecimal totalCost = materialTotal.add(operationTotal);

        // Рекомендуемая цена: наценка 80% (можно вынести в настройки)
        BigDecimal sellingPrice = totalCost.multiply(BigDecimal.valueOf(1.8));

        // Маржа в процентах
        BigDecimal margin = sellingPrice.subtract(totalCost)
                .divide(totalCost, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return new CalculationResult(
                materialTotal,
                operationTotal,
                totalCost,
                sellingPrice,
                margin,
                breakdown
        );
    }

    /**
     * Вычисляет количество с отходами по формуле.
     * Если формула не задана, используется базовое quantity (из ProductMaterial).
     */
    private BigDecimal evaluateQuantity(String formula, JexlContext context, BigDecimal wasteCoefficient, BigDecimal defaultQuantity) {
        BigDecimal baseQty;

        if (formula == null || formula.trim().isEmpty()) {
            // Если формулы нет, используем quantity из ProductMaterial (или 1)
            baseQty = defaultQuantity != null ? defaultQuantity : BigDecimal.ONE;
        } else {
            try {
                JexlExpression expr = new JexlBuilder().create().createExpression(formula);
                Object result = expr.evaluate(context);
                if (result instanceof BigDecimal) {
                    baseQty = (BigDecimal) result;
                } else if (result instanceof Number num) {
                    baseQty = BigDecimal.valueOf(num.doubleValue());
                } else {
                    baseQty = defaultQuantity != null ? defaultQuantity : BigDecimal.ONE;
                }
            } catch (Exception e) {
                baseQty = defaultQuantity != null ? defaultQuantity : BigDecimal.ONE;
            }
        }

        // Умножаем на коэффициент отхода
        return baseQty.multiply(wasteCoefficient);
    }
}
