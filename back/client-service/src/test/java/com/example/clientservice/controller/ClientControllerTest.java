package com.example.clientservice.controller;

import com.example.clientservice.config.SecurityConfig;
import com.example.clientservice.dto.ClientCreateRequest;
import com.example.clientservice.dto.ClientResponse;
import com.example.clientservice.dto.ClientUpdateRequest;
import com.example.clientservice.entity.Client;
import com.example.clientservice.entity.ClientType;
import com.example.clientservice.exception.NotFoundException;
import com.example.clientservice.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private ClientResponse clientResponse() {
        ClientResponse response = new ClientResponse();
        response.setId(1L);
        response.setName("ООО Высота");
        response.setType("COMPANY");
        response.setContactPerson("Иванов Иван");
        response.setPhone("+7(900)123-45-67");
        response.setEmail("info@example.com");
        response.setInn("7700000001");
        response.setAddress("г. Москва");
        response.setPriceplus(new BigDecimal("5.50"));
        response.setCompanyId(10L);
        return response;
    }

    // ===================== GET /api/v1/clients =====================

    @Nested
    @DisplayName("GET /api/v1/clients")
    class GetAllClients {

        @Test
        @DisplayName("Returns paginated list of clients")
        @WithMockUser(roles = "ADMIN")
        void returnsPaginatedClients() throws Exception {
            Page<ClientResponse> page = new PageImpl<>(
                    List.of(clientResponse()), PageRequest.of(0, 20), 1);

            when(clientService.getAllClients(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/clients")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("ООО Высота"))
                    .andExpect(jsonPath("$.content[0].type").value("COMPANY"))
                    .andExpect(jsonPath("$.content[0].priceplus").value(5.50))
                    .andExpect(jsonPath("$.content[0].companyId").value(10))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Filters by search query q")
        @WithMockUser(roles = "MANAGER")
        void filtersBySearchQuery() throws Exception {
            Page<ClientResponse> emptyPage = new PageImpl<>(List.of());
            when(clientService.getAllClients(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/clients")
                            .param("q", "высота"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("ACCOUNTANT can list clients")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCanListClients() throws Exception {
            when(clientService.getAllClients(any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(clientResponse())));

            mockMvc.perform(get("/api/v1/clients"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("ООО Высота"));
        }

        @Test
        @DisplayName("PRODUCTION cannot list clients")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotListClients() throws Exception {
            mockMvc.perform(get("/api/v1/clients"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/clients/search =====================

    @Nested
    @DisplayName("GET /api/v1/clients/search")
    class SearchClients {

        @Test
        @DisplayName("Returns matching clients")
        @WithMockUser(roles = "ADMIN")
        void returnsMatchingClients() throws Exception {
            Client client = new Client();
            client.setId(1L);
            client.setName("Иванов Иван");
            client.setType(ClientType.PRIVATE);
            client.setContactPerson("Иванов Иван");
            client.setPriceplus(BigDecimal.TEN);

            when(clientService.searchClients(eq("Иван"), isNull())).thenReturn(List.of(client));

            mockMvc.perform(get("/api/v1/clients/search")
                            .param("q", "Иван"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].name").value("Иванов Иван"))
                    .andExpect(jsonPath("$[0].type").value("PRIVATE"));
        }

        @Test
        @DisplayName("ACCOUNTANT can search clients")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCanSearchClients() throws Exception {
            when(clientService.searchClients(eq("нет"), isNull())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/clients/search")
                            .param("q", "нет"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("PRODUCTION cannot search clients")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotSearchClients() throws Exception {
            mockMvc.perform(get("/api/v1/clients/search")
                            .param("q", "Иван"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/clients/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/clients/{id}")
    class GetClientById {

        @Test
        @DisplayName("Returns client by ID")
        @WithMockUser(roles = "MANAGER")
        void returnsClientById() throws Exception {
            when(clientService.getClientById(1L)).thenReturn(clientResponse());

            mockMvc.perform(get("/api/v1/clients/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ООО Высота"))
                    .andExpect(jsonPath("$.contactPerson").value("Иванов Иван"))
                    .andExpect(jsonPath("$.inn").value("7700000001"))
                    .andExpect(jsonPath("$.priceplus").value(5.50));
        }

        @Test
        @DisplayName("Returns 404 for non-existent client")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            when(clientService.getClientById(999L))
                    .thenThrow(new NotFoundException("Клиент не найден"));

            mockMvc.perform(get("/api/v1/clients/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("PRODUCTION cannot get client")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotGetClient() throws Exception {
            mockMvc.perform(get("/api/v1/clients/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/clients =====================

    @Nested
    @DisplayName("POST /api/v1/clients")
    class CreateClient {

        @Test
        @DisplayName("MANAGER can create client")
        @WithMockUser(roles = "MANAGER")
        void managerCanCreateClient() throws Exception {
            ClientCreateRequest request = new ClientCreateRequest();
            request.setName("Новый клиент");
            request.setType("PRIVATE");
            request.setContactPerson("Петров Пётр");
            request.setPhone("+7(903)987-65-43");
            request.setEmail("new@example.com");
            request.setPriceplus(new BigDecimal("3.00"));
            request.setCompanyId(10L);

            when(clientService.createClient(any())).thenReturn(clientResponse());

            mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ООО Высота"))
                    .andExpect(jsonPath("$.priceplus").value(5.50));
        }

        @Test
        @DisplayName("Returns 400 for malformed JSON body")
        @WithMockUser(roles = "ADMIN")
        void returns400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot create client")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotCreateClient() throws Exception {
            ClientCreateRequest request = new ClientCreateRequest();
            request.setName("Запрещённый");

            mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot create client")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotCreateClient() throws Exception {
            ClientCreateRequest request = new ClientCreateRequest();
            request.setName("Запрещённый");

            mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/clients/{id} =====================

    @Nested
    @DisplayName("PUT /api/v1/clients/{id}")
    class UpdateClient {

        @Test
        @DisplayName("ADMIN can update client")
        @WithMockUser(roles = "ADMIN")
        void adminCanUpdateClient() throws Exception {
            ClientUpdateRequest request = new ClientUpdateRequest();
            request.setName("Обновлённый клиент");
            request.setType("COMPANY");
            request.setPriceplus(new BigDecimal("7.00"));

            ClientResponse updated = clientResponse();
            updated.setName("Обновлённый клиент");

            when(clientService.updateClient(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/clients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Обновлённый клиент"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent client")
        @WithMockUser(roles = "GOD")
        void returns404ForNonExistent() throws Exception {
            ClientUpdateRequest request = new ClientUpdateRequest();
            request.setName("Обновлённый клиент");

            when(clientService.updateClient(eq(999L), any()))
                    .thenThrow(new NotFoundException("Клиент не найден"));

            mockMvc.perform(put("/api/v1/clients/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot update client")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotUpdateClient() throws Exception {
            ClientUpdateRequest request = new ClientUpdateRequest();
            request.setName("Запрещённый");

            mockMvc.perform(put("/api/v1/clients/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== DELETE /api/v1/clients/{id} =====================

    @Nested
    @DisplayName("DELETE /api/v1/clients/{id}")
    class DeleteClient {

        @Test
        @DisplayName("ADMIN can delete client")
        @WithMockUser(roles = "ADMIN")
        void adminCanDeleteClient() throws Exception {
            doNothing().when(clientService).deleteClient(1L);

            mockMvc.perform(delete("/api/v1/clients/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("MANAGER can delete client")
        @WithMockUser(roles = "MANAGER")
        void managerCanDeleteClient() throws Exception {
            doNothing().when(clientService).deleteClient(1L);

            mockMvc.perform(delete("/api/v1/clients/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 for non-existent client")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            org.mockito.Mockito.doThrow(new NotFoundException("Клиент не найден"))
                    .when(clientService).deleteClient(999L);

            mockMvc.perform(delete("/api/v1/clients/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot delete client")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotDeleteClient() throws Exception {
            mockMvc.perform(delete("/api/v1/clients/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot delete client")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotDeleteClient() throws Exception {
            mockMvc.perform(delete("/api/v1/clients/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Unauthenticated users get 401 on list")
        @WithAnonymousUser
        void unauthenticatedListGets401() throws Exception {
            mockMvc.perform(get("/api/v1/clients"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on get by id")
        @WithAnonymousUser
        void unauthenticatedGetByIdGets401() throws Exception {
            mockMvc.perform(get("/api/v1/clients/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on create")
        @WithAnonymousUser
        void unauthenticatedCreateGets401() throws Exception {
            mockMvc.perform(post("/api/v1/clients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ClientCreateRequest())))
                    .andExpect(status().isUnauthorized());
        }
    }
}
