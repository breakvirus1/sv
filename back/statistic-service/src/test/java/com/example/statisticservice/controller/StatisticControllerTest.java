package com.example.statisticservice.controller;

import com.example.statisticservice.config.SecurityConfig;
import com.example.statisticservice.dto.MaterialExpenseRow;
import com.example.statisticservice.service.OrderSyncService;
import com.example.statisticservice.service.StatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StatisticController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class StatisticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatisticsService statisticsService;

    @MockBean
    private OrderSyncService orderSyncService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private MaterialExpenseRow materialExpenseRow() {
        MaterialExpenseRow row = new MaterialExpenseRow();
        row.setType("material");
        row.setName("Баннер");
        row.setMaterialName("Баннер 3x4");
        row.setUnit("м2");
        row.setNetQuantity(new BigDecimal("10.00"));
        row.setQuantityWithWaste(new BigDecimal("11.00"));
        row.setCost(new BigDecimal("1000.00"));
        row.setConsumptionWithPriceplus(new BigDecimal("1100.00"));
        return row;
    }

    // ===================== GET /api/v1/admin/statistics/material-expense =====================

    @Nested
    @DisplayName("GET /api/v1/admin/statistics/material-expense")
    class GetMaterialExpense {

        @Test
        @DisplayName("ADMIN gets 200 with material expense statistics")
        @WithMockUser(roles = "ADMIN")
        void adminGetsStatistics() throws Exception {
            when(statisticsService.getMaterialExpenseStatistics(any(), any(), isNull()))
                    .thenReturn(List.of(materialExpenseRow()));

            mockMvc.perform(get("/api/v1/admin/statistics/material-expense")
                            .param("fromDate", "2026-01-01")
                            .param("toDate", "2026-01-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("material"))
                    .andExpect(jsonPath("$[0].name").value("Баннер"))
                    .andExpect(jsonPath("$[0].cost").value(1000.00))
                    .andExpect(jsonPath("$[0].consumptionWithPriceplus").value(1100.00));
        }
    }

    // ===================== POST /api/v1/admin/statistics/sync-order =====================

    @Nested
    @DisplayName("POST /api/v1/admin/statistics/sync-order")
    class SyncOrder {

        @Test
        @DisplayName("ADMIN gets 200 when order is synchronized")
        @WithMockUser(roles = "ADMIN")
        void adminCanSyncOrder() throws Exception {
            mockMvc.perform(post("/api/v1/admin/statistics/sync-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("id", 1))))
                    .andExpect(status().isOk());

            verify(orderSyncService).syncOrder(any());
        }

        @Test
        @DisplayName("MANAGER gets 200 when order is synchronized")
        @WithMockUser(roles = "MANAGER")
        void managerCanSyncOrder() throws Exception {
            mockMvc.perform(post("/api/v1/admin/statistics/sync-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("id", 1))))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PRODUCTION gets 403 and cannot synchronize order")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotSyncOrder() throws Exception {
            mockMvc.perform(post("/api/v1/admin/statistics/sync-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("id", 1))))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== DELETE /api/v1/admin/statistics/sync-order/{orderId} =====================

    @Nested
    @DisplayName("DELETE /api/v1/admin/statistics/sync-order/{orderId}")
    class DeleteOrderFromStatistics {

        @Test
        @DisplayName("ADMIN gets 200 when order is deleted from statistics")
        @WithMockUser(roles = "ADMIN")
        void adminCanDeleteOrder() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/statistics/sync-order/1"))
                    .andExpect(status().isOk());

            verify(orderSyncService).deleteByOrderId(eq(1L));
        }

        @Test
        @DisplayName("USER gets 403 and cannot delete order from statistics")
        @WithMockUser(roles = "USER")
        void userCannotDeleteOrder() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/statistics/sync-order/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous gets 401 on GET material-expense")
        @WithAnonymousUser
        void anonymousGetGets401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/statistics/material-expense"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on POST sync-order")
        @WithAnonymousUser
        void anonymousPostGets401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/statistics/sync-order")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous gets 401 on DELETE sync-order/{orderId}")
        @WithAnonymousUser
        void anonymousDeleteGets401() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/statistics/sync-order/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
