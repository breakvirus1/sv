package com.example.orderservice.controller;

import com.example.orderservice.dto.*;
import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.orderservice.config.SecurityConfig;
import com.example.orderservice.controller.OrderController;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtDecoder jwtDecoder;

    // ===================== GET /api/v1/orders =====================

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetAllOrders {

        @Test
        @DisplayName("Returns paginated list of orders")
        @WithMockUser(roles = "ADMIN")
        void returnsPaginatedOrders() throws Exception {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setId(1L);
            orderResponse.setOrderNumber("20260526102500001");
            orderResponse.setTotalAmount(new BigDecimal("1000.00"));

            Page<OrderResponse> page = new PageImpl<>(List.of(orderResponse), PageRequest.of(0, 20), 1);

            when(orderService.getAllOrders(any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/orders")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].orderNumber").value("20260526102500001"))
                    .andExpect(jsonPath("$.content[0].totalAmount").value(1000.00));
        }

        @Test
        @DisplayName("Filters by status")
        @WithMockUser(roles = "MANAGER")
        void filtersByStatus() throws Exception {
            Page<OrderResponse> emptyPage = new PageImpl<>(List.of());
            when(orderService.getAllOrders(any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/orders")
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ACCOUNTANT can list orders")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCanListOrders() throws Exception {
            Page<OrderResponse> emptyPage = new PageImpl<>(List.of());
            when(orderService.getAllOrders(any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isOk());
        }
    }

    // ===================== GET /api/v1/orders/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/orders/{id}")
    class GetOrderById {

        @Test
        @DisplayName("Returns order by numeric ID")
        @WithMockUser(roles = "ADMIN")
        void returnsOrderByNumericId() throws Exception {
            OrderResponse response = new OrderResponse();
            response.setId(1L);
            response.setOrderNumber("20260526102500001");
            response.setTotalAmount(new BigDecimal("1000.00"));
            response.setPaidAmount(new BigDecimal("500.00"));
            response.setDebtAmount(new BigDecimal("500.00"));

            when(orderService.getOrderById(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.orderNumber").value("20260526102500001"))
                    .andExpect(jsonPath("$.totalAmount").value(1000.00))
                    .andExpect(jsonPath("$.paidAmount").value(500.00))
                    .andExpect(jsonPath("$.debtAmount").value(500.00));
        }

        @Test
        @DisplayName("Returns 404 for non-existent order")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            when(orderService.getOrderById(999L))
                    .thenThrow(new NotFoundException("Заказ не найден"));

            mockMvc.perform(get("/api/v1/orders/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== GET /api/v1/orders/number/{orderNumber} =====================

    @Nested
    @DisplayName("GET /api/v1/orders/number/{orderNumber}")
    class GetOrderByNumber {

        @Test
        @DisplayName("Returns order by orderNumber")
        @WithMockUser(roles = "ADMIN")
        void returnsOrderByNumber() throws Exception {
            OrderResponse response = new OrderResponse();
            response.setId(1L);
            response.setOrderNumber("20260526102500001");
            response.setTotalAmount(new BigDecimal("1000.00"));

            when(orderService.getOrderByOrderNumber("20260526102500001")).thenReturn(response);

            mockMvc.perform(get("/api/v1/orders/number/20260526102500001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderNumber").value("20260526102500001"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent orderNumber")
        @WithMockUser(roles = "MANAGER")
        void returns404ForNonExistent() throws Exception {
            when(orderService.getOrderByOrderNumber("NONEXISTENT"))
                    .thenThrow(new NotFoundException("Заказ не найден"));

            mockMvc.perform(get("/api/v1/orders/number/NONEXISTENT"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /api/v1/orders =====================

    @Nested
    @DisplayName("POST /api/v1/orders")
    class CreateOrder {

        @Test
        @DisplayName("PRODUCTION cannot create order")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotCreateOrder() throws Exception {
            OrderCreateRequest request = new OrderCreateRequest();
            request.setClientId(1L);

            mockMvc.perform(post("/api/v1/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/orders/{id} =====================

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}")
    class UpdateOrder {

        @Test
        @DisplayName("ACCOUNTANT cannot update order")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotUpdateOrder() throws Exception {
            OrderUpdateRequest request = new OrderUpdateRequest();
            request.setDescription("Updated description");

            mockMvc.perform(put("/api/v1/orders/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/orders/{id}/status =====================

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}/status")
    class UpdateStatus {

    }

    // ===================== PUT /api/v1/orders/{id}/stage =====================

    @Nested
    @DisplayName("PUT /api/v1/orders/{id}/stage")
    class UpdateStage {

        @Test
        @DisplayName("MANAGER cannot update production stage")
        @WithMockUser(roles = "MANAGER")
        void managerCannotUpdateStage() throws Exception {
            mockMvc.perform(put("/api/v1/orders/1/stage")
                            .param("stage", "IN_PROGRESS"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/orders/{id}/calculated =====================

    @Nested
    @DisplayName("GET /api/v1/orders/{id}/calculated")
    class GetCalculatedOrder {

        @Test
        @DisplayName("Returns calculated order with priceplus")
        @WithMockUser(roles = "ADMIN")
        void returnsCalculatedOrder() throws Exception {
            CalculatedOrderResponse response = new CalculatedOrderResponse();
            response.setOrderId(1L);
            response.setPriceplus(new BigDecimal("10.00"));
            response.setTotalWithoutPriceplus(new BigDecimal("1000.00"));
            response.setTotalWithPriceplus(new BigDecimal("1100.00"));

            MaterialCalculation matCalc = new MaterialCalculation();
            matCalc.setId(1L);
            matCalc.setMaterialName("Баннер");
            matCalc.setCost(new BigDecimal("800.00"));
            matCalc.setCostPriceplus(new BigDecimal("880.00"));
            response.setMaterials(List.of(matCalc));

            when(orderService.getCalculatedOrder(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/orders/1/calculated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value(1))
                    .andExpect(jsonPath("$.priceplus").value(10.00))
                    .andExpect(jsonPath("$.totalWithoutPriceplus").value(1000.00))
                    .andExpect(jsonPath("$.totalWithPriceplus").value(1100.00))
                    .andExpect(jsonPath("$.materials[0].cost").value(800.00))
                    .andExpect(jsonPath("$.materials[0].costPriceplus").value(880.00));
        }

        @Test
        @DisplayName("Returns 404 for non-existent order")
        @WithMockUser(roles = "MANAGER")
        void returns404ForNonExistent() throws Exception {
            when(orderService.getCalculatedOrder(999L))
                    .thenThrow(new NotFoundException("Заказ не найден"));

            mockMvc.perform(get("/api/v1/orders/999/calculated"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ACCOUNTANT can access calculated order")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCanAccess() throws Exception {
            CalculatedOrderResponse response = new CalculatedOrderResponse();
            response.setOrderId(1L);
            response.setTotalWithoutPriceplus(BigDecimal.ZERO);
            response.setTotalWithPriceplus(BigDecimal.ZERO);
            response.setMaterials(List.of());

            when(orderService.getCalculatedOrder(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/orders/1/calculated"))
                    .andExpect(status().isOk());
        }
    }

    // ===================== POST /api/v1/orders/{id}/payments =====================

    @Nested
    @DisplayName("POST /api/v1/orders/{id}/payments")
    class AddPayment {

        @Test
        @DisplayName("MANAGER cannot add payment")
        @WithMockUser(roles = "MANAGER")
        void managerCannotAddPayment() throws Exception {
            PaymentRequest request = new PaymentRequest();
            request.setAmount(new BigDecimal("100.00"));

            mockMvc.perform(post("/api/v1/orders/1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Unauthenticated users get 401")
        void unauthenticatedUsersGet401() throws Exception {
            mockMvc.perform(get("/api/v1/orders"))
                    .andExpect(status().isUnauthorized());
        }

    }
}
