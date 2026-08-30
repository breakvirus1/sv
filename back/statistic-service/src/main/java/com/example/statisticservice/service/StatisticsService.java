package com.example.statisticservice.service;

import com.example.statisticservice.dto.MaterialExpenseRow;
import com.example.statisticservice.entity.StatisticMaterialConsumption;
import com.example.statisticservice.entity.StatisticOrder;
import com.example.statisticservice.entity.StatisticOrderItem;
import com.example.statisticservice.repository.StatisticMaterialConsumptionRepository;
import com.example.statisticservice.repository.StatisticOrderItemRepository;
import com.example.statisticservice.repository.StatisticOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticOrderRepository statisticOrderRepository;
    private final StatisticOrderItemRepository statisticOrderItemRepository;
    private final StatisticMaterialConsumptionRepository statisticMaterialConsumptionRepository;

    @Transactional(readOnly = true)
    public Page<MaterialExpenseRow> getMaterialExpenseStatistics(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Specification<StatisticOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StatisticOrder> ordersPage = statisticOrderRepository.findAll(spec, pageable);

        return ordersPage.map(order -> {
            List<StatisticOrderItem> items = statisticOrderItemRepository.findByStatisticOrderId(order.getId());
            List<StatisticMaterialConsumption> allConsumptions = new ArrayList<>();
            for (StatisticOrderItem item : items) {
                allConsumptions.addAll(statisticMaterialConsumptionRepository.findByStatisticOrderItemId(item.getId()));
            }

            Map<String, List<StatisticMaterialConsumption>> grouped = allConsumptions.stream()
                    .collect(Collectors.groupingBy(c -> c.getMaterialName() != null ? c.getMaterialName() : ""));

            List<MaterialExpenseRow> rows = new ArrayList<>();
            for (Map.Entry<String, List<StatisticMaterialConsumption>> entry : grouped.entrySet()) {
                String materialName = entry.getKey();
                List<StatisticMaterialConsumption> consumptions = entry.getValue();

                BigDecimal netQuantity = consumptions.stream()
                        .map(StatisticMaterialConsumption::getNetQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal consumptionWithPriceplus = consumptions.stream()
                        .map(StatisticMaterialConsumption::getConsumptionWithPriceplus)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal cost = consumptions.stream()
                        .map(StatisticMaterialConsumption::getCost)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal consumptionFromOrderPositions = consumptions.stream()
                        .map(StatisticMaterialConsumption::getCostPriceplus)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                rows.add(new MaterialExpenseRow(
                        materialName,
                        netQuantity,
                        consumptionWithPriceplus,
                        cost,
                        consumptionFromOrderPositions
                ));
            }

            MaterialExpenseRow result = new MaterialExpenseRow();
            result.setMaterialName("Total");
            result.setNetQuantity(rows.stream().map(MaterialExpenseRow::getNetQuantity).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.setConsumptionWithPriceplus(rows.stream().map(MaterialExpenseRow::getConsumptionWithPriceplus).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.setCost(rows.stream().map(MaterialExpenseRow::getCost).reduce(BigDecimal.ZERO, BigDecimal::add));
            result.setConsumptionFromOrderPositions(rows.stream().map(MaterialExpenseRow::getConsumptionFromOrderPositions).reduce(BigDecimal.ZERO, BigDecimal::add));
            return result;
        });
    }
}
