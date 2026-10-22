package com.example.employeeservice.controller;

import com.example.employeeservice.config.SecurityConfig;
import com.example.employeeservice.dto.EmployeeCreateRequest;
import com.example.employeeservice.dto.EmployeeResponse;
import com.example.employeeservice.dto.EmployeeUpdateRequest;
import com.example.employeeservice.exception.NotFoundException;
import com.example.employeeservice.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmployeeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private EmployeeResponse sampleEmployee() {
        EmployeeResponse response = new EmployeeResponse();
        response.setId(1L);
        response.setFullName("Иванов Иван");
        response.setUsername("ivanov");
        response.setPosition("Менеджер");
        response.setEmail("ivanov@example.com");
        response.setCompanyId(1L);
        response.setRoles(List.of("ROLE_ADMIN"));
        return response;
    }

    // ===================== GET /api/v1/employees =====================

    @Nested
    @DisplayName("GET /api/v1/employees")
    class GetAllEmployees {

        @Test
        @DisplayName("ADMIN can list employees (paginated)")
        @WithMockUser(roles = "ADMIN")
        void adminCanListEmployees() throws Exception {
            Page<EmployeeResponse> page = new PageImpl<>(
                    List.of(sampleEmployee()), PageRequest.of(0, 20), 1);

            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/employees")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].fullName").value("Иванов Иван"))
                    .andExpect(jsonPath("$.content[0].username").value("ivanov"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("MANAGER can list employees")
        @WithMockUser(roles = "MANAGER")
        void managerCanListEmployees() throws Exception {
            Page<EmployeeResponse> page = new PageImpl<>(List.of(sampleEmployee()));
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].username").value("ivanov"));
        }

        @Test
        @DisplayName("PRODUCTION can list employees")
        @WithMockUser(roles = "PRODUCTION")
        void productionCanListEmployees() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("GOD can list employees")
        @WithMockUser(roles = "GOD")
        void godCanListEmployees() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot list employees")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotListEmployees() throws Exception {
            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Filters by query param q")
        @WithMockUser(roles = "ADMIN")
        void filtersByQuery() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees")
                            .param("q", "ivan"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("Uses company_id from JWT claim when param is absent (string claim)")
        void usesCompanyIdFromJwtStringClaim() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin").claim("company_id", "1"))))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> companyCaptor = ArgumentCaptor.forClass(Long.class);
            verify(employeeService).getAllEmployees(any(), any(), companyCaptor.capture());
            assertEquals(1L, companyCaptor.getValue());
        }

        @Test
        @DisplayName("Uses company_id from JWT claim when param is absent (numeric claim)")
        void usesCompanyIdFromNumericJwtClaim() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin").claim("company_id", 2))))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> companyCaptor = ArgumentCaptor.forClass(Long.class);
            verify(employeeService).getAllEmployees(any(), any(), companyCaptor.capture());
            assertEquals(2L, companyCaptor.getValue());
        }

        @Test
        @DisplayName("JWT companyId wins over explicit param for non-GOD")
        void jwtCompanyOverridesExplicitParam() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees")
                            .param("companyId", "3")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin").claim("company_id", "1"))))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> companyCaptor = ArgumentCaptor.forClass(Long.class);
            verify(employeeService).getAllEmployees(any(), any(), companyCaptor.capture());
            assertEquals(1L, companyCaptor.getValue());
        }

        @Test
        @DisplayName("Mock user without JWT passes null companyId")
        @WithMockUser(roles = "ADMIN")
        void mockUserWithoutJwtPassesNullCompanyId() throws Exception {
            Page<EmployeeResponse> emptyPage = new PageImpl<>(List.of());
            when(employeeService.getAllEmployees(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk());

            ArgumentCaptor<Long> companyCaptor = ArgumentCaptor.forClass(Long.class);
            verify(employeeService).getAllEmployees(any(), any(), companyCaptor.capture());
            assertNull(companyCaptor.getValue());
        }
    }

    // ===================== GET /api/v1/employees/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/employees/{id}")
    class GetEmployeeById {

        @Test
        @DisplayName("Returns employee by ID")
        @WithMockUser(roles = "ADMIN")
        void returnsEmployeeById() throws Exception {
            when(employeeService.getEmployeeById(eq(1L), any())).thenReturn(sampleEmployee());

            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.fullName").value("Иванов Иван"))
                    .andExpect(jsonPath("$.username").value("ivanov"))
                    .andExpect(jsonPath("$.companyId").value(1));
        }

        @Test
        @DisplayName("Returns 404 for non-existent employee")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            when(employeeService.getEmployeeById(eq(999L), any()))
                    .thenThrow(new NotFoundException("Сотрудник не найден"));

            mockMvc.perform(get("/api/v1/employees/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("MANAGER can get employee by ID")
        @WithMockUser(roles = "MANAGER")
        void managerCanGetEmployeeById() throws Exception {
            when(employeeService.getEmployeeById(eq(1L), any())).thenReturn(sampleEmployee());

            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("PRODUCTION cannot get employee by ID")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotGetEmployeeById() throws Exception {
            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/employees/username/{username} =====================

    @Nested
    @DisplayName("GET /api/v1/employees/username/{username}")
    class GetEmployeeByUsername {

        @Test
        @DisplayName("Returns employee by username")
        @WithMockUser(roles = "ADMIN")
        void returnsEmployeeByUsername() throws Exception {
            when(employeeService.getEmployeeByUsername(eq("ivanov"), any())).thenReturn(sampleEmployee());

            mockMvc.perform(get("/api/v1/employees/username/ivanov"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("ivanov"));
        }
    }

    // ===================== GET /api/v1/employees/roles/{username} =====================

    @Nested
    @DisplayName("GET /api/v1/employees/roles/{username}")
    class GetEmployeeRoles {

        @Test
        @DisplayName("Returns Keycloak roles for user")
        @WithMockUser(roles = "ADMIN")
        void returnsEmployeeRoles() throws Exception {
            when(employeeService.getKeycloakRolesForUser("ivanov"))
                    .thenReturn(List.of("ADMIN", "MANAGER"));

            mockMvc.perform(get("/api/v1/employees/roles/ivanov"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("ADMIN"))
                    .andExpect(jsonPath("$[1]").value("MANAGER"));
        }
    }

    // ===================== POST /api/v1/employees =====================

    @Nested
    @DisplayName("POST /api/v1/employees")
    class CreateEmployee {

        @Test
        @DisplayName("ADMIN can create employee")
        @WithMockUser(roles = "ADMIN")
        void adminCanCreateEmployee() throws Exception {
            EmployeeCreateRequest request = new EmployeeCreateRequest();
            request.setFullName("Иванов Иван");
            request.setUsername("ivanov");
            request.setPosition("Менеджер");
            request.setCompanyId(1L);

            when(employeeService.createEmployee(any())).thenReturn(sampleEmployee());

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.username").value("ivanov"));
        }

        @Test
        @DisplayName("MANAGER cannot create employee")
        @WithMockUser(roles = "MANAGER")
        void managerCannotCreateEmployee() throws Exception {
            EmployeeCreateRequest request = new EmployeeCreateRequest();
            request.setFullName("Иван Иванов");
            request.setUsername("ivanov");

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot create employee")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotCreateEmployee() throws Exception {
            EmployeeCreateRequest request = new EmployeeCreateRequest();
            request.setFullName("Иван Иванов");
            request.setUsername("ivanov");

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/employees/{id} =====================

    @Nested
    @DisplayName("PUT /api/v1/employees/{id}")
    class UpdateEmployee {

        @Test
        @DisplayName("ADMIN can update employee")
        @WithMockUser(roles = "ADMIN")
        void adminCanUpdateEmployee() throws Exception {
            EmployeeUpdateRequest request = new EmployeeUpdateRequest();
            request.setFullName("Updated Name");
            request.setUsername("ivanov");

            when(employeeService.updateEmployee(eq(1L), any(), any())).thenReturn(sampleEmployee());

            mockMvc.perform(put("/api/v1/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("MANAGER cannot update employee")
        @WithMockUser(roles = "MANAGER")
        void managerCannotUpdateEmployee() throws Exception {
            EmployeeUpdateRequest request = new EmployeeUpdateRequest();
            request.setFullName("Updated Name");
            request.setUsername("ivanov");

            mockMvc.perform(put("/api/v1/employees/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== DELETE /api/v1/employees/{id} =====================

    @Nested
    @DisplayName("DELETE /api/v1/employees/{id}")
    class DeleteEmployee {

        @Test
        @DisplayName("ADMIN can delete employee")
        @WithMockUser(roles = "ADMIN")
        void adminCanDeleteEmployee() throws Exception {
            doNothing().when(employeeService).deleteEmployee(eq(1L), any());

            mockMvc.perform(delete("/api/v1/employees/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("GOD can delete employee")
        @WithMockUser(roles = "GOD")
        void godCanDeleteEmployee() throws Exception {
            doNothing().when(employeeService).deleteEmployee(eq(1L), any());

            mockMvc.perform(delete("/api/v1/employees/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("MANAGER cannot delete employee")
        @WithMockUser(roles = "MANAGER")
        void managerCannotDeleteEmployee() throws Exception {
            mockMvc.perform(delete("/api/v1/employees/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot delete employee")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotDeleteEmployee() throws Exception {
            mockMvc.perform(delete("/api/v1/employees/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/employees/sync =====================

    @Nested
    @DisplayName("POST /api/v1/employees/sync")
    class SyncCurrentEmployee {

        @Test
        @DisplayName("Authenticated user can sync self")
        @WithMockUser(roles = "USER")
        void authenticatedUserCanSync() throws Exception {
            when(employeeService.syncOrCreateFromKeycloak(any())).thenReturn(sampleEmployee());

            mockMvc.perform(post("/api/v1/employees/sync"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("ivanov"));
        }

        @Test
        @DisplayName("Passes JWT with company_id claim to service")
        void passesJwtWithCompanyIdToService() throws Exception {
            when(employeeService.syncOrCreateFromKeycloak(any())).thenReturn(sampleEmployee());

            mockMvc.perform(post("/api/v1/employees/sync")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                    .jwt(j -> j.subject("admin").claim("company_id", "7"))))
                    .andExpect(status().isOk());

            ArgumentCaptor<Jwt> jwtCaptor = ArgumentCaptor.forClass(Jwt.class);
            verify(employeeService).syncOrCreateFromKeycloak(jwtCaptor.capture());
            assertEquals("7", jwtCaptor.getValue().getClaim("company_id"));
        }
    }

    // ===================== POST /api/v1/employees/sync-all =====================

    @Nested
    @DisplayName("POST /api/v1/employees/sync-all")
    class SyncAllFromKeycloak {

        @Test
        @DisplayName("ADMIN syncs only their own company (from JWT claim)")
        void adminSyncsOwnCompany() throws Exception {
            when(employeeService.syncFromKeycloakByCompany(any())).thenReturn("created=0, updated=0");

            mockMvc.perform(post("/api/v1/employees/sync-all")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject("admin")
                                            .claim("company_id", "5")
                                            .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                    .andExpect(status().isOk());

            verify(employeeService).syncFromKeycloakByCompany(5L);
        }

        @Test
        @DisplayName("GOD syncs all companies (companyId nullified)")
        void godSyncsAllCompanies() throws Exception {
            when(employeeService.syncFromKeycloakByCompany(any())).thenReturn("created=0, updated=0");

            mockMvc.perform(post("/api/v1/employees/sync-all")
                            .with(jwt()
                                    .authorities(new SimpleGrantedAuthority("ROLE_GOD"))
                                    .jwt(j -> j.subject("root")
                                            .claim("company_id", "5")
                                            .claim("realm_access.roles", List.of("GOD")))))
                    .andExpect(status().isOk());

            verify(employeeService).syncFromKeycloakByCompany(null);
        }

        @Test
        @DisplayName("MANAGER cannot sync all")
        @WithMockUser(roles = "MANAGER")
        void managerCannotSyncAll() throws Exception {
            mockMvc.perform(post("/api/v1/employees/sync-all"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot sync all")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotSyncAll() throws Exception {
            mockMvc.perform(post("/api/v1/employees/sync-all"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/employees/sync-by-company =====================

    @Nested
    @DisplayName("POST /api/v1/employees/sync-by-company")
    class SyncByCompany {

        @Test
        @DisplayName("ADMIN can sync by company")
        @WithMockUser(roles = "ADMIN")
        void adminCanSyncByCompany() throws Exception {
            when(employeeService.syncFromKeycloakByCompany(10L)).thenReturn("created=1, updated=2");

            mockMvc.perform(post("/api/v1/employees/sync-by-company")
                            .param("companyId", "10"))
                    .andExpect(status().isOk());

            verify(employeeService).syncFromKeycloakByCompany(10L);
        }

        @Test
        @DisplayName("MANAGER cannot sync by company")
        @WithMockUser(roles = "MANAGER")
        void managerCannotSyncByCompany() throws Exception {
            mockMvc.perform(post("/api/v1/employees/sync-by-company")
                            .param("companyId", "10"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("PRODUCTION cannot sync by company")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotSyncByCompany() throws Exception {
            mockMvc.perform(post("/api/v1/employees/sync-by-company")
                            .param("companyId", "10"))
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
            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on get by id")
        @WithAnonymousUser
        void unauthenticatedGetByIdGets401() throws Exception {
            mockMvc.perform(get("/api/v1/employees/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on sync")
        @WithAnonymousUser
        void unauthenticatedSyncGets401() throws Exception {
            mockMvc.perform(post("/api/v1/employees/sync"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
