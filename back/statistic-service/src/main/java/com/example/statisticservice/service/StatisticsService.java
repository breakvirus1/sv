package com.example.statisticservice.service;

import com.example.statisticservice.dto.MaterialExpenseRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<MaterialExpenseRow> getMaterialExpenseStatistics(LocalDate fromDate, LocalDate toDate) {
        String materialSql = """
            SELECT
                'material' as row_type,
                COALESCE(m.name, '') as name,
                '' as material_name,
                COALESCE(MAX(m.unit), '') as unit,
                COALESCE(SUM(om.quantity / NULLIF(om.waste_coefficient, 0)), 0) as net_quantity,
                COALESCE(SUM(om.quantity), 0) as quantity_with_waste,
                COALESCE(SUM(CASE WHEN m.unit = 'шт' THEN om.quantity ELSE 0 END), 0) as pieces,
                COALESCE(SUM(CASE WHEN m.unit IN ('п.м.', 'м') THEN om.quantity ELSE 0 END), 0) as linear_meters,
                COALESCE(SUM(CASE WHEN m.unit = 'м2' THEN om.quantity ELSE 0 END), 0) as square_meters,
                COALESCE(SUM(CASE WHEN m.unit = 'шт' AND om.eyelet_cost > 0 THEN om.quantity ELSE 0 END), 0) as eyelet_pieces,
                COALESCE(SUM(om.cost * (1 + COALESCE(o.priceplus, 0) / 100)), 0) as consumption_with_priceplus,
                COALESCE(SUM(om.cost), 0) as cost,
                COALESCE(SUM(om.cost_priceplus), 0) as consumption_from_order_positions,
                0 as operation_count,
                0 as operations_total_cost
            FROM svschema.orders o
            JOIN svschema.order_items oi ON oi.order_id = o.id
            JOIN svschema.order_materials om ON om.order_item_id = oi.id
            LEFT JOIN svschema.materials m ON m.id = om.material_id
            WHERE COALESCE(o.deleted, false) = false
              AND COALESCE(oi.deleted, false) = false
              AND COALESCE(om.deleted, false) = false
            """;

        String operationSql = """
            SELECT
                'operation' as row_type,
                oo.operation_name as name,
                COALESCE(m.name, '') as material_name,
                COALESCE(MAX(m.unit), '') as unit,
                0 as net_quantity,
                0 as quantity_with_waste,
                COALESCE(SUM(CASE WHEN m.unit = 'шт' THEN oo.calculated_quantity ELSE 0 END), 0) as pieces,
                COALESCE(SUM(CASE WHEN m.unit IN ('п.м.', 'м') THEN oo.calculated_quantity ELSE 0 END), 0) as linear_meters,
                COALESCE(SUM(CASE WHEN m.unit = 'м2' THEN oo.calculated_quantity ELSE 0 END), 0) as square_meters,
                0 as eyelet_pieces,
                0 as consumption_with_priceplus,
                0 as cost,
                0 as consumption_from_order_positions,
                COUNT(*) as operation_count,
                COALESCE(SUM(oo.subtotal), 0) as operations_total_cost
            FROM svschema.orders o
            JOIN svschema.order_items oi ON oi.order_id = o.id
            JOIN svschema.order_item_operations oo ON oo.order_item_id = oi.id
            LEFT JOIN svschema.order_materials om ON om.order_item_id = oi.id
            LEFT JOIN svschema.materials m ON m.id = om.material_id
            WHERE COALESCE(o.deleted, false) = false
              AND COALESCE(oi.deleted, false) = false
              AND COALESCE(oo.deleted, false) = false
            """;

        if (fromDate != null) {
            materialSql += " AND o.order_date >= :fromDate";
            operationSql += " AND o.order_date >= :fromDate";
        }
        if (toDate != null) {
            materialSql += " AND o.order_date <= :toDate";
            operationSql += " AND o.order_date <= :toDate";
        }

        materialSql += " GROUP BY m.name, m.unit";
        operationSql += " GROUP BY oo.operation_name, m.name, m.unit";

        String unionSql = materialSql + " UNION ALL " + operationSql + " ORDER BY row_type, name";

        Query query = entityManager.createNativeQuery(unionSql);
        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream()
                .map(row -> new MaterialExpenseRow(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (String) row[3],
                        (BigDecimal) row[4],
                        (BigDecimal) row[5],
                        (BigDecimal) row[6],
                        (BigDecimal) row[7],
                        (BigDecimal) row[8],
                        (BigDecimal) row[9],
                        (BigDecimal) row[10],
                        (BigDecimal) row[11],
                        (BigDecimal) row[12],
                        ((Number) row[13]).longValue(),
                        (BigDecimal) row[14]
                ))
                .collect(Collectors.toList());
    }
}
