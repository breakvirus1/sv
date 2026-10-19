package com.example.clientservice.controller;

import com.example.clientservice.config.SecurityConfig;
import com.example.clientservice.dto.CompanyCreateRequest;
import com.example.clientservice.dto.CompanyResponse;
import com.example.clientservice.exception.NotFoundException;
import com.example.clientservice.service.CompanyService;
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

@WebMvcTest(controllers = CompanyController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class CompanyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private CompanyResponse companyResponse() {
        CompanyResponse response = new CompanyResponse();
        response.setId(1L);
        response.setName("ООО Вектор");
        return response;
    }

    // ===================== GET /api/v1/companies =====================

    @Nested
    @DisplayName("GET /api/v1/companies")
    class GetAllCompanies {

        @Test
        @DisplayName("Returns paginated list of companies")
        @WithMockUser(roles = "ADMIN")
        void returnsPaginatedCompanies() throws Exception {
            Page<CompanyResponse> page = new PageImpl<>(
                    List.of(companyResponse()), PageRequest.of(0, 20), 1);

            when(companyService.getAllCompanies(any(), any(), isNull())).thenReturn(page);

            mockMvc.perform(get("/api/v1/companies")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("ООО Вектор"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Filters by search query q")
        @WithMockUser(roles = "GOD")
        void filtersBySearchQuery() throws Exception {
            when(companyService.getAllCompanies(any(), any(), isNull()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/companies")
                            .param("q", "вектор"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("MANAGER cannot list companies")
        @WithMockUser(roles = "MANAGER")
        void managerCannotListCompanies() throws Exception {
            mockMvc.perform(get("/api/v1/companies"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot list companies")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotListCompanies() throws Exception {
            mockMvc.perform(get("/api/v1/companies"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/companies/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/companies/{id}")
    class GetCompanyById {

        @Test
        @DisplayName("Returns company by ID")
        @WithMockUser(roles = "ADMIN")
        void returnsCompanyById() throws Exception {
            when(companyService.getCompanyById(1L)).thenReturn(companyResponse());

            mockMvc.perform(get("/api/v1/companies/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ООО Вектор"));
        }

        @Test
        @DisplayName("GOD can get company")
        @WithMockUser(roles = "GOD")
        void godCanGetCompany() throws Exception {
            when(companyService.getCompanyById(1L)).thenReturn(companyResponse());

            mockMvc.perform(get("/api/v1/companies/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("ООО Вектор"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent company")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            when(companyService.getCompanyById(999L))
                    .thenThrow(new NotFoundException("Компания не найдена"));

            mockMvc.perform(get("/api/v1/companies/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("MANAGER cannot get company")
        @WithMockUser(roles = "MANAGER")
        void managerCannotGetCompany() throws Exception {
            mockMvc.perform(get("/api/v1/companies/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/companies =====================

    @Nested
    @DisplayName("POST /api/v1/companies")
    class CreateCompany {

        @Test
        @DisplayName("ADMIN can create company")
        @WithMockUser(roles = "ADMIN")
        void adminCanCreateCompany() throws Exception {
            CompanyCreateRequest request = new CompanyCreateRequest("ООО Вектор");

            when(companyService.createCompany(any())).thenReturn(companyResponse());

            mockMvc.perform(post("/api/v1/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ООО Вектор"));
        }

        @Test
        @DisplayName("Returns 400 for malformed JSON body")
        @WithMockUser(roles = "ADMIN")
        void returns400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/api/v1/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\": "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("MANAGER cannot create company")
        @WithMockUser(roles = "MANAGER")
        void managerCannotCreateCompany() throws Exception {
            mockMvc.perform(post("/api/v1/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompanyCreateRequest("ООО Вектор"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/companies/{id} =====================

    @Nested
    @DisplayName("PUT /api/v1/companies/{id}")
    class UpdateCompany {

        @Test
        @DisplayName("ADMIN can update company")
        @WithMockUser(roles = "ADMIN")
        void adminCanUpdateCompany() throws Exception {
            CompanyCreateRequest request = new CompanyCreateRequest("ООО Вектор 2");
            CompanyResponse updated = companyResponse();
            updated.setName("ООО Вектор 2");

            when(companyService.updateCompany(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(put("/api/v1/companies/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("ООО Вектор 2"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent company")
        @WithMockUser(roles = "GOD")
        void returns404ForNonExistent() throws Exception {
            CompanyCreateRequest request = new CompanyCreateRequest("ООО Вектор 2");

            when(companyService.updateCompany(eq(999L), any()))
                    .thenThrow(new NotFoundException("Компания не найдена"));

            mockMvc.perform(put("/api/v1/companies/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("MANAGER cannot update company")
        @WithMockUser(roles = "MANAGER")
        void managerCannotUpdateCompany() throws Exception {
            mockMvc.perform(put("/api/v1/companies/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompanyCreateRequest("ООО Вектор 2"))))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== DELETE /api/v1/companies/{id} =====================

    @Nested
    @DisplayName("DELETE /api/v1/companies/{id}")
    class DeleteCompany {

        @Test
        @DisplayName("ADMIN can delete company")
        @WithMockUser(roles = "ADMIN")
        void adminCanDeleteCompany() throws Exception {
            doNothing().when(companyService).deleteCompany(1L);

            mockMvc.perform(delete("/api/v1/companies/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Returns 404 for non-existent company")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistent() throws Exception {
            org.mockito.Mockito.doThrow(new NotFoundException("Компания не найдена"))
                    .when(companyService).deleteCompany(999L);

            mockMvc.perform(delete("/api/v1/companies/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("MANAGER cannot delete company")
        @WithMockUser(roles = "MANAGER")
        void managerCannotDeleteCompany() throws Exception {
            mockMvc.perform(delete("/api/v1/companies/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT cannot delete company")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantCannotDeleteCompany() throws Exception {
            mockMvc.perform(delete("/api/v1/companies/1"))
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
            mockMvc.perform(get("/api/v1/companies"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on create")
        @WithAnonymousUser
        void unauthenticatedCreateGets401() throws Exception {
            mockMvc.perform(post("/api/v1/companies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CompanyCreateRequest("ООО Вектор"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated users get 401 on delete")
        @WithAnonymousUser
        void unauthenticatedDeleteGets401() throws Exception {
            mockMvc.perform(delete("/api/v1/companies/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
