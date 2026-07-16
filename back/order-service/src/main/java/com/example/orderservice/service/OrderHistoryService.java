package com.example.orderservice.service;

import com.example.orderservice.entity.Order;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {

    private final OrderRepository orderRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public void logUpdate(Long orderId, String description, Order oldOrder, Order newOrder, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.orderservice.exception.NotFoundException("Заказ не найден"));

        String timestamp = LocalDateTime.now().format(FORMATTER);
        StringBuilder entry = new StringBuilder();
        entry.append("[").append(timestamp).append("] ");
        if (username != null && !username.isBlank()) {
            entry.append("Пользователь: ").append(username).append("; ");
        }

        StringBuilder changes = new StringBuilder();

        if (!Objects.equals(description, oldOrder.getDescription())) {
            changes.append("Описание: \"").append(quote(oldOrder.getDescription())).append("\" → \"").append(quote(description)).append("\"; ");
        }
        if (!Objects.equals(newOrder.getOrderDate(), oldOrder.getOrderDate())) {
            changes.append("Дата заказа: ").append(oldOrder.getOrderDate()).append(" → ").append(newOrder.getOrderDate()).append("; ");
        }
        if (!Objects.equals(newOrder.getDueDate(), oldOrder.getDueDate())) {
            changes.append("Срок сдачи: ").append(oldOrder.getDueDate()).append(" → ").append(newOrder.getDueDate()).append("; ");
        }
        if (!Objects.equals(newOrder.getManager(), oldOrder.getManager())) {
            String oldManager = oldOrder.getManager() != null ? oldOrder.getManager().getFullName() : "не назначен";
            String newManager = newOrder.getManager() != null ? newOrder.getManager().getFullName() : "не назначен";
            changes.append("Менеджер: ").append(oldManager).append(" → ").append(newManager).append("; ");
        }
        if (!Objects.equals(newOrder.getPriceplus(), oldOrder.getPriceplus())) {
            changes.append("Наценка: ").append(oldOrder.getPriceplus()).append("% → ").append(newOrder.getPriceplus()).append("%; ");
        }
        if (!Objects.equals(newOrder.getTotalAmount(), oldOrder.getTotalAmount())) {
            changes.append("Сумма: ").append(oldOrder.getTotalAmount()).append(" → ").append(newOrder.getTotalAmount()).append("; ");
        }
        if (!Objects.equals(newOrder.getTotalWithPriceplus(), oldOrder.getTotalWithPriceplus())) {
            changes.append("Сумма с наценкой: ").append(oldOrder.getTotalWithPriceplus()).append(" → ").append(newOrder.getTotalWithPriceplus()).append("; ");
        }
        if (!Objects.equals(newOrder.getStatus(), oldOrder.getStatus())) {
            changes.append("Статус: ").append(oldOrder.getStatus()).append(" → ").append(newOrder.getStatus()).append("; ");
        }
        if (!Objects.equals(newOrder.getProductionStage(), oldOrder.getProductionStage())) {
            changes.append("Стадия производства: ").append(oldOrder.getProductionStage()).append(" → ").append(newOrder.getProductionStage()).append("; ");
        }

        if (changes.length() == 0) {
            return;
        }

        entry.append(changes.toString().trim());
        if (entry.charAt(entry.length() - 1) == ';') {
            entry.setLength(entry.length() - 1);
        }

        String oldHistory = order.getHistory();
        if (oldHistory == null || oldHistory.isBlank()) {
            order.setHistory(entry.toString());
        } else {
            order.setHistory(oldHistory + "\n" + entry.toString());
        }

        orderRepository.save(order);
    }

    @Transactional
    public void logCreation(Long orderId, String orderNumber, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.orderservice.exception.NotFoundException("Заказ не найден"));

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] " +
                (username != null && !username.isBlank() ? "Пользователь: " + username + "; " : "") +
                "Заказ создан. Номер: " + orderNumber;

        String oldHistory = order.getHistory();
        if (oldHistory == null || oldHistory.isBlank()) {
            order.setHistory(entry);
        } else {
            order.setHistory(oldHistory + "\n" + entry);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void logPayment(Long orderId, BigDecimal amount, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.orderservice.exception.NotFoundException("Заказ не найден"));

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] " +
                (username != null && !username.isBlank() ? "Пользователь: " + username + "; " : "") +
                "Добавлена оплата: " + amount + " ₽";

        String oldHistory = order.getHistory();
        if (oldHistory == null || oldHistory.isBlank()) {
            order.setHistory(entry);
        } else {
            order.setHistory(oldHistory + "\n" + entry);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void logComment(Long orderId, String commentText, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.orderservice.exception.NotFoundException("Заказ не найден"));

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] " +
                (username != null && !username.isBlank() ? "Пользователь: " + username + "; " : "") +
                "Добавлен комментарий: \"" + quote(commentText) + "\"";

        String oldHistory = order.getHistory();
        if (oldHistory == null || oldHistory.isBlank()) {
            order.setHistory(entry);
        } else {
            order.setHistory(oldHistory + "\n" + entry);
        }
        orderRepository.save(order);
    }

    @Transactional
    public void logStageUpdate(Long orderId, String stageName, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new com.example.orderservice.exception.NotFoundException("Заказ не найден"));

        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] " +
                (username != null && !username.isBlank() ? "Пользователь: " + username + "; " : "") +
                "Стадия производства изменена на: " + stageName;

        String oldHistory = order.getHistory();
        if (oldHistory == null || oldHistory.isBlank()) {
            order.setHistory(entry);
        } else {
            order.setHistory(oldHistory + "\n" + entry);
        }
        orderRepository.save(order);
    }

    private String quote(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
