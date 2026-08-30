package com.example.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticSyncService {

    private final RestTemplate restTemplate;

    @Value("${statistic.service.url:http://localhost:8089/api/v1/admin/statistics}")
    private String statisticServiceUrl;

    public void syncOrder(com.example.orderservice.entity.Order order) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String jwtToken = null;
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                jwtToken = ((Jwt) auth.getPrincipal()).getTokenValue();
            }

            Map<String, Object> orderData = new HashMap<>();
            orderData.put("id", order.getId());
            orderData.put("orderNumber", order.getOrderNumber());
            orderData.put("orderDate", order.getOrderDate() != null ? order.getOrderDate().toString() : null);
            orderData.put("totalAmount", order.getTotalAmount());
            orderData.put("priceplus", order.getPriceplus());
            orderData.put("totalWithPriceplus", order.getTotalWithPriceplus());
            orderData.put("status", order.getStatus() != null ? order.getStatus().name() : null);
            orderData.put("productionStage", order.getProductionStage() != null ? order.getProductionStage().name() : null);

            if (order.getClient() != null) {
                Map<String, Object> client = new HashMap<>();
                client.put("id", order.getClient().getId());
                client.put("name", order.getClient().getName());
                orderData.put("client", client);
            } else {
                orderData.put("client", null);
            }

            if (order.getManager() != null) {
                Map<String, Object> manager = new HashMap<>();
                manager.put("id", order.getManager().getId());
                manager.put("fullName", order.getManager().getFullName());
                orderData.put("manager", manager);
            } else {
                orderData.put("manager", null);
            }

            List<Map<String, Object>> items = new ArrayList<>();
            if (order.getItems() != null) {
                for (com.example.orderservice.entity.OrderItem item : order.getItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("name", item.getName());
                    itemMap.put("price", item.getPrice());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("cost", item.getCost());
                    itemMap.put("readyDate", item.getReadyDate() != null ? item.getReadyDate().toString() : null);

                    List<Map<String, Object>> materials = new ArrayList<>();
                    if (item.getMaterials() != null) {
                        for (com.example.orderservice.entity.OrderMaterial om : item.getMaterials()) {
                            Map<String, Object> matMap = new HashMap<>();
                            matMap.put("id", om.getId());
                            matMap.put("quantity", om.getQuantity());
                            matMap.put("wasteCoefficient", om.getWasteCoefficient());
                            matMap.put("cost", om.getCost());
                            matMap.put("costPriceplus", om.getCostPriceplus());
                            matMap.put("eyeletCost", om.getEyeletCost());
                            matMap.put("widthM", om.getWidthM());
                            matMap.put("heightM", om.getHeightM());

                            if (om.getMaterial() != null) {
                                Map<String, Object> material = new HashMap<>();
                                material.put("id", om.getMaterial().getId());
                                material.put("name", om.getMaterial().getName());
                                material.put("unit", om.getMaterial().getUnit());
                                material.put("price", om.getMaterial().getPrice());
                                matMap.put("material", material);
                            } else {
                                matMap.put("material", null);
                            }

                            materials.add(matMap);
                        }
                    }
                    itemMap.put("materials", materials);
                    items.add(itemMap);
                }
            }
            orderData.put("items", items);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null) {
                headers.setBearerAuth(jwtToken);
            }

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(orderData, headers);
            restTemplate.exchange(
                    statisticServiceUrl + "/sync-order",
                    HttpMethod.POST,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("Failed to sync order with statistic service: " + e.getMessage());
        }
    }

    public void deleteOrderFromStatistics(Long orderId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String jwtToken = null;
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                jwtToken = ((Jwt) auth.getPrincipal()).getTokenValue();
            }

            HttpHeaders headers = new HttpHeaders();
            if (jwtToken != null) {
                headers.setBearerAuth(jwtToken);
            }

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            restTemplate.exchange(
                    statisticServiceUrl + "/sync-order/" + orderId,
                    HttpMethod.DELETE,
                    requestEntity,
                    Void.class
            );
        } catch (Exception e) {
            System.err.println("Failed to delete order from statistic service: " + e.getMessage());
        }
    }
}
