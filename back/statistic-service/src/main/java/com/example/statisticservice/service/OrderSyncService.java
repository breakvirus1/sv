package com.example.statisticservice.service;

import com.example.statisticservice.entity.StatisticMaterialConsumption;
import com.example.statisticservice.entity.StatisticOrder;
import com.example.statisticservice.entity.StatisticOrderItem;
import com.example.statisticservice.repository.StatisticMaterialConsumptionRepository;
import com.example.statisticservice.repository.StatisticOrderItemRepository;
import com.example.statisticservice.repository.StatisticOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderSyncService {

    private final StatisticOrderRepository statisticOrderRepository;
    private final StatisticOrderItemRepository statisticOrderItemRepository;
    private final StatisticMaterialConsumptionRepository statisticMaterialConsumptionRepository;

    @Transactional
    public void syncOrder(Map<String, Object> orderData) {
        Long orderId = ((Number) orderData.get("id")).longValue();
        String orderNumber = (String) orderData.get("orderNumber");
        String orderDateStr = (String) orderData.get("orderDate");
        java.time.LocalDate orderDate = orderDateStr != null ? java.time.LocalDate.parse(orderDateStr) : null;

        Map<String, Object> client = (Map<String, Object>) orderData.get("client");
        Long clientId = client != null ? ((Number) client.get("id")).longValue() : null;
        String clientName = client != null ? client.get("name") != null ? client.get("name").toString() : null : null;

        BigDecimal totalAmount = orderData.get("totalAmount") != null ? new BigDecimal(orderData.get("totalAmount").toString()) : BigDecimal.ZERO;
        BigDecimal priceplus = orderData.get("priceplus") != null ? new BigDecimal(orderData.get("priceplus").toString()) : BigDecimal.ZERO;
        BigDecimal totalWithPriceplus = orderData.get("totalWithPriceplus") != null ? new BigDecimal(orderData.get("totalWithPriceplus").toString()) : BigDecimal.ZERO;
        String status = (String) orderData.get("status");
        String productionStage = (String) orderData.get("productionStage");

        Map<String, Object> manager = (Map<String, Object>) orderData.get("manager");
        Long managerId = manager != null ? ((Number) manager.get("id")).longValue() : null;
        String managerName = manager != null ? manager.get("fullName") != null ? manager.get("fullName").toString() : null : null;

        StatisticOrder statisticOrder = statisticOrderRepository.findByOrderId(orderId)
                .orElseGet(StatisticOrder::new);
        statisticOrder.setOrderId(orderId);
        statisticOrder.setOrderNumber(orderNumber);
        statisticOrder.setOrderDate(orderDate);
        statisticOrder.setClientId(clientId);
        statisticOrder.setClientName(clientName);
        statisticOrder.setTotalAmount(totalAmount);
        statisticOrder.setPriceplus(priceplus);
        statisticOrder.setTotalWithPriceplus(totalWithPriceplus);
        statisticOrder.setStatus(status);
        statisticOrder.setProductionStage(productionStage);
        statisticOrder.setManagerId(managerId);
        statisticOrder.setManagerName(managerName);
        statisticOrder.setSyncedAt(LocalDateTime.now());

        StatisticOrder savedOrder = statisticOrderRepository.save(statisticOrder);

        List<Map<String, Object>> items = (List<Map<String, Object>>) orderData.get("items");
        if (items != null) {
            Set<Long> existingItemIds = new HashSet<>();
            for (Map<String, Object> itemData : items) {
                Long orderItemId = ((Number) itemData.get("id")).longValue();
                existingItemIds.add(orderItemId);

                StatisticOrderItem orderItem = statisticOrderItemRepository.findByOrderItemId(orderItemId)
                        .orElseGet(StatisticOrderItem::new);
                orderItem.setStatisticOrder(savedOrder);
                orderItem.setOrderItemId(orderItemId);
                orderItem.setName((String) itemData.get("name"));
                orderItem.setPrice(itemData.get("price") != null ? new BigDecimal(itemData.get("price").toString()) : BigDecimal.ZERO);
                orderItem.setQuantity(itemData.get("quantity") != null ? ((Number) itemData.get("quantity")).intValue() : 1);
                orderItem.setCost(itemData.get("cost") != null ? new BigDecimal(itemData.get("cost").toString()) : BigDecimal.ZERO);
                String readyDateStr = (String) itemData.get("readyDate");
                orderItem.setReadyDate(readyDateStr != null ? java.time.LocalDate.parse(readyDateStr) : null);

                StatisticOrderItem savedItem = statisticOrderItemRepository.save(orderItem);

                List<Map<String, Object>> materials = (List<Map<String, Object>>) itemData.get("materials");
                if (materials != null) {
                    Set<Long> existingMaterialIds = new HashSet<>();
                    for (Map<String, Object> matData : materials) {
                        Long orderMaterialId = ((Number) matData.get("id")).longValue();
                        existingMaterialIds.add(orderMaterialId);

                        StatisticMaterialConsumption consumption = statisticMaterialConsumptionRepository.findByOrderMaterialId(orderMaterialId)
                                .orElseGet(StatisticMaterialConsumption::new);
                        consumption.setStatisticOrderItem(savedItem);
                        consumption.setOrderMaterialId(orderMaterialId);

                        Map<String, Object> material = (Map<String, Object>) matData.get("material");
                        Long materialId = material != null ? ((Number) material.get("id")).longValue() : null;
                        String materialName = material != null ? material.get("name") != null ? material.get("name").toString() : null : null;
                        String materialUnit = material != null ? material.get("unit") != null ? material.get("unit").toString() : null : null;
                        BigDecimal materialPrice = material != null && material.get("price") != null ? new BigDecimal(material.get("price").toString()) : BigDecimal.ZERO;

                        consumption.setMaterialId(materialId);
                        consumption.setMaterialName(materialName);
                        consumption.setMaterialUnit(materialUnit);
                        consumption.setMaterialPrice(materialPrice);
                        consumption.setWasteCoefficient(matData.get("wasteCoefficient") != null ? new BigDecimal(matData.get("wasteCoefficient").toString()) : BigDecimal.ONE);
                        consumption.setQuantity(matData.get("quantity") != null ? new BigDecimal(matData.get("quantity").toString()) : BigDecimal.ZERO);
                        consumption.setWidthM(matData.get("widthM") != null ? new BigDecimal(matData.get("widthM").toString()) : null);
                        consumption.setHeightM(matData.get("heightM") != null ? new BigDecimal(matData.get("heightM").toString()) : null);

                        BigDecimal wasteCoefficient = consumption.getWasteCoefficient();
                        BigDecimal quantity = consumption.getQuantity();
                        BigDecimal netQuantity = quantity.divide(wasteCoefficient, 4, BigDecimal.ROUND_HALF_UP);
                        consumption.setNetQuantity(netQuantity);

                        BigDecimal cost = matData.get("cost") != null ? new BigDecimal(matData.get("cost").toString()) : BigDecimal.ZERO;
                        consumption.setCost(cost);
                        consumption.setCostPriceplus(matData.get("costPriceplus") != null ? new BigDecimal(matData.get("costPriceplus").toString()) : BigDecimal.ZERO);
                        consumption.setEyeletCost(matData.get("eyeletCost") != null ? new BigDecimal(matData.get("eyeletCost").toString()) : BigDecimal.ZERO);

                        BigDecimal orderPriceplus = savedOrder.getPriceplus() != null ? savedOrder.getPriceplus() : BigDecimal.ZERO;
                        consumption.setOrderClientPriceplus(orderPriceplus);
                        BigDecimal consumptionWithPriceplus = cost.multiply(BigDecimal.ONE.add(orderPriceplus.divide(BigDecimal.valueOf(100), 4, BigDecimal.ROUND_HALF_UP)));
                        consumption.setConsumptionWithPriceplus(consumptionWithPriceplus);
                        consumption.setSyncedAt(LocalDateTime.now());

                        statisticMaterialConsumptionRepository.save(consumption);
                    }

                    List<StatisticMaterialConsumption> existingConsumptions = statisticMaterialConsumptionRepository.findByStatisticOrderItemId(savedItem.getId());
                    for (StatisticMaterialConsumption existing : existingConsumptions) {
                        if (!existingMaterialIds.contains(existing.getOrderMaterialId())) {
                            statisticMaterialConsumptionRepository.delete(existing);
                        }
                    }
                }
            }

            List<StatisticOrderItem> existingItems = statisticOrderItemRepository.findByStatisticOrderId(savedOrder.getId());
            for (StatisticOrderItem existing : existingItems) {
                if (!existingItemIds.contains(existing.getOrderItemId())) {
                    statisticOrderItemRepository.delete(existing);
                }
            }
        }
    }

    @Transactional
    public void deleteByOrderId(Long orderId) {
        List<StatisticMaterialConsumption> consumptions = statisticMaterialConsumptionRepository.findAll()
                .stream()
                .filter(c -> {
                    StatisticOrderItem item = c.getStatisticOrderItem();
                    if (item == null) return false;
                    StatisticOrder order = item.getStatisticOrder();
                    return order != null && order.getOrderId().equals(orderId);
                })
                .toList();
        statisticMaterialConsumptionRepository.deleteAll(consumptions);

        List<StatisticOrderItem> items = statisticOrderItemRepository.findAll()
                .stream()
                .filter(i -> {
                    StatisticOrder order = i.getStatisticOrder();
                    return order != null && order.getOrderId().equals(orderId);
                })
                .toList();
        statisticOrderItemRepository.deleteAll(items);

        statisticOrderRepository.findByOrderId(orderId).ifPresent(statisticOrderRepository::delete);
    }
}
