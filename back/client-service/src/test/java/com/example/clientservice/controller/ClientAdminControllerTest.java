package com.example.clientservice.controller;

import com.example.clientservice.config.SecurityConfig;
import com.example.clientservice.entity.Client;
import com.example.clientservice.entity.ClientType;
import com.example.clientservice.service.ClientAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientAdminController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class ClientAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientAdminService clientAdminService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Client client() {
        Client client = new Client();
        client.setId(1L);
        client.setName("ООО Вектор 1");
        client.setType(ClientType.COMPANY);
        client.setContactPerson("Иванов Иван");
        client.setPhone("+7(900)123-45-67");
        client.setEmail("test1@example.com");
        client.setInn("7700000000");
        client.setAddress("г. Москва, ул. Примерная, д. 1");
        client.setPriceplus(new BigDecimal("5.00"));
        client.setCompanyId(10L);
        return client;
    }

    // ===================== POST /api/v1/admin/clients/generate =====================

    @Nested
    @DisplayName("POST /api/v1/admin/clients/generate")
    class GenerateTestClients {

        @Test
        @DisplayName("ADMIN can generate test clients")
        @WithMockUser(roles = "ADMIN")
        void adminCanGenerateTestClients() throws Exception {
            when(clientAdminService.generateTestClients(20))
                    .thenReturn(List.of(client(), client()));

            mockMvc.perform(post("/api/v1/admin/clients/generate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("ООО Вектор 1"))
                    .andExpect(jsonPath("$[0].type").value("COMPANY"))
                    .andExpect(jsonPath("$[0].inn").value("7700000000"));
        }

        @Test
        @DisplayName("MANAGER cannot generate test clients")
        @WithMockUser(roles = "MANAGER")
        void managerCannotGenerateTestClients() throws Exception {
            mockMvc.perform(post("/api/v1/admin/clients/generate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot generate test clients")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotGenerateTestClients() throws Exception {
            mockMvc.perform(post("/api/v1/admin/clients/generate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated users get 401")
        @WithAnonymousUser
        void unauthenticatedGets401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/clients/generate"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
