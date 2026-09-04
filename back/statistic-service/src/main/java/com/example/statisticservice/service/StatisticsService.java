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
                CAST(COALESCE(SUM(om.quantity / NULLIF(om.waste_coefficient, 0)), 0) AS NUMERIC) as net_quantity,
                CAST(COALESCE(SUM(om.quantity), 0) AS NUMERIC) as quantity_with_waste,
                CAST(COALESCE(SUM(CASE WHEN m.unit = 'шт' THEN om.quantity ELSE 0 END), 0) AS NUMERIC) as pieces,
                CAST(COALESCE(SUM(CASE WHEN m.unit IN ('п.м.', 'м') THEN om.quantity ELSE 0 END), 0) AS NUMERIC) as linear_meters,
                CAST(COALESCE(SUM(CASE WHEN m.unit = 'м2' THEN om.quantity ELSE 0 END), 0) AS NUMERIC) as square_meters,
                CAST(0 AS NUMERIC) as eyelet_pieces,
                CAST(COALESCE(SUM(om.cost * (1 + COALESCE(o.priceplus, 0) / 100)), 0) AS NUMERIC) as consumption_with_priceplus,
                CAST(COALESCE(SUM(om.cost), 0) AS NUMERIC) as cost,
                CAST(COALESCE(SUM(om.cost_priceplus), 0) AS NUMERIC) as consumption_from_order_positions,
                CAST(0 AS NUMERIC) as operation_count,
                CAST(0 AS NUMERIC) as operations_total_cost
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
                CASE
                    WHEN oo.operation_name ILIKE '%%люверс%%' OR oo.operation_name ILIKE '%%установка%%'
                    THEN 'шт'
                    ELSE COALESCE(MAX(m.unit), '')
                END as unit,
                CAST(0 AS NUMERIC) as net_quantity,
                CAST(0 AS NUMERIC) as quantity_with_waste,
                CAST(COALESCE(SUM(CASE
                    WHEN m.unit = 'шт'
                        OR oo.operation_name ILIKE '%%люверс%%'
                        OR oo.operation_name ILIKE '%%установка%%'
                    THEN oo.calculated_quantity
                    ELSE 0
                END), 0) AS NUMERIC) as pieces,
                CAST(COALESCE(SUM(CASE
                    WHEN m.unit IN ('п.м.', 'м')
                        AND NOT (oo.operation_name ILIKE '%%люверс%%' OR oo.operation_name ILIKE '%%установка%%')
                    THEN oo.calculated_quantity
                    ELSE 0
                END), 0) AS NUMERIC) as linear_meters,
                CAST(COALESCE(SUM(CASE
                    WHEN m.unit = 'м2'
                        AND NOT (oo.operation_name ILIKE '%%люверс%%' OR oo.operation_name ILIKE '%%установка%%')
                    THEN oo.calculated_quantity
                    ELSE 0
                END), 0) AS NUMERIC) as square_meters,
                CAST(0 AS NUMERIC) as eyelet_pieces,
                CAST(0 AS NUMERIC) as consumption_with_priceplus,
                CAST(0 AS NUMERIC) as cost,
                CAST(0 AS NUMERIC) as consumption_from_order_positions,
                CAST(COUNT(*) AS NUMERIC) as operation_count,
                CAST(COALESCE(SUM(oo.subtotal), 0) AS NUMERIC) as operations_total_cost
            FROM svschema.orders o
            JOIN svschema.order_items oi ON oi.order_id = o.id
            JOIN svschema.order_item_operations oo ON oo.order_item_id = oi.id
            LEFT JOIN svschema.order_materials om ON om.order_item_id = oi.id
            LEFT JOIN svschema.materials m ON m.id = om.material_id
            WHERE COALESCE(o.deleted, false) = false
              AND COALESCE(oi.deleted, false) = false
              AND COALESCE(oo.deleted, false) = false
            """;

        String eyeletSql = """
            SELECT
                'eyelet' as row_type,
                'Люверсы' as name,
                '' as material_name,
                'шт' as unit,
                CAST(0 AS NUMERIC) as net_quantity,
                CAST(0 AS NUMERIC) as quantity_with_waste,
                CAST(0 AS NUMERIC) as pieces,
                CAST(0 AS NUMERIC) as linear_meters,
                CAST(0 AS NUMERIC) as square_meters,
                CAST(COALESCE(SUM(om.eyelet_quantity), 0) AS NUMERIC) as eyelet_pieces,
                CAST(0 AS NUMERIC) as consumption_with_priceplus,
                CAST(0 AS NUMERIC) as cost,
                CAST(0 AS NUMERIC) as consumption_from_order_positions,
                CAST(0 AS NUMERIC) as operation_count,
                CAST(0 AS NUMERIC) as operations_total_cost
            FROM svschema.orders o
            JOIN svschema.order_items oi ON oi.order_id = o.id
            JOIN (
                SELECT DISTINCT order_item_id, eyelet_quantity
                FROM svschema.order_materials
                WHERE eyelet_cost > 0
                  AND eyelet_quantity > 0
                  AND deleted = false
            ) om ON om.order_item_id = oi.id
            WHERE COALESCE(o.deleted, false) = false
              AND COALESCE(oi.deleted, false) = false
            """;

        if (fromDate != null) {
            materialSql += " AND o.order_date >= ?";
            operationSql += " AND o.order_date >= ?";
            eyeletSql += " AND o.order_date >= ?";
        }
        if (toDate != null) {
            materialSql += " AND o.order_date <= ?";
            operationSql += " AND o.order_date <= ?";
            eyeletSql += " AND o.order_date <= ?";
        }

        materialSql += " GROUP BY m.name, m.unit";
        operationSql += " GROUP BY oo.operation_name, m.name, m.unit";

        String unionSql = materialSql + " UNION ALL " + operationSql + " UNION ALL " + eyeletSql + " ORDER BY row_type, name";

        System.out.println("STATISTIC SQL: " + unionSql);
        Query query = entityManager.createNativeQuery(unionSql);
        int paramIndex = 1;
        if (fromDate != null && toDate != null) {
            query.setParameter(paramIndex++, fromDate);
            query.setParameter(paramIndex++, toDate);
            query.setParameter(paramIndex++, fromDate);
            query.setParameter(paramIndex++, toDate);
            query.setParameter(paramIndex++, fromDate);
            query.setParameter(paramIndex++, toDate);
        } else if (fromDate != null) {
            query.setParameter(paramIndex++, fromDate);
            query.setParameter(paramIndex++, fromDate);
            query.setParameter(paramIndex++, fromDate);
        } else if (toDate != null) {
            query.setParameter(paramIndex++, toDate);
            query.setParameter(paramIndex++, toDate);
            query.setParameter(paramIndex++, toDate);
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
