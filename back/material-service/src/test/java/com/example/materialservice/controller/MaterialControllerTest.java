package com.example.materialservice.controller;

import com.example.materialservice.config.SecurityConfig;
import com.example.materialservice.dto.MaterialCreateRequest;
import com.example.materialservice.dto.MaterialResponse;
import com.example.materialservice.dto.MaterialUpdateRequest;
import com.example.materialservice.exception.NotFoundException;
import com.example.materialservice.service.MaterialService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialController.class)
@Import({SecurityConfig.class, MaterialControllerTest.AccessDeniedTestAdvice.class})
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class MaterialControllerTest {

    @ControllerAdvice
    static class AccessDeniedTestAdvice {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Void> handleAccessDenied(AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MaterialService materialService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private MaterialResponse materialResponse() {
        return new MaterialResponse(1L, "Баннер 440 г/м2", "м2", new BigDecimal("150.00"),
                new BigDecimal("1.10"), new BigDecimal("1.00"), null);
    }

    private MaterialCreateRequest createRequest() {
        return new MaterialCreateRequest("Баннер 440 г/м2", "м2", new BigDecimal("150.00"),
                new BigDecimal("1.10"), new BigDecimal("1.00"), null);
    }

    // ===================== GET /api/v1/materials =====================

    @Nested
    @DisplayName("GET /api/v1/materials")
    class GetAllMaterials {

        @Test
        @DisplayName("ADMIN gets paginated list of materials")
        @WithMockUser(roles = "ADMIN")
        void adminGetsMaterials() throws Exception {
            Page<MaterialResponse> page = new PageImpl<>(List.of(materialResponse()), PageRequest.of(0, 20), 1);
            when(materialService.getAllMaterials(any(), any(), isNull())).thenReturn(page);

            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Баннер 440 г/м2"))
                    .andExpect(jsonPath("$.content[0].price").value(150.00))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("MANAGER gets materials with search query")
        @WithMockUser(roles = "MANAGER")
        void managerGetsMaterialsWithQuery() throws Exception {
            Page<MaterialResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
            when(materialService.getAllMaterials(any(), any(), isNull())).thenReturn(page);

            mockMvc.perform(get("/api/v1/materials").param("q", "баннер"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));

            verify(materialService).getAllMaterials(any(), any(), isNull());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT gets 403")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantGets403() throws Exception {
            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/materials/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/materials/{id}")
    class GetMaterialById {

        @Test
        @DisplayName("ADMIN gets material by id")
        @WithMockUser(roles = "ADMIN")
        void adminGetsMaterial() throws Exception {
            when(materialService.getMaterialById(1L)).thenReturn(materialResponse());

            mockMvc.perform(get("/api/v1/materials/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Баннер 440 г/м2"))
                    .andExpect(jsonPath("$.unit").value("м2"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent material")
        @WithMockUser(roles = "MANAGER")
        void returns404ForNonExistent() throws Exception {
            when(materialService.getMaterialById(999L))
                    .thenThrow(new NotFoundException("Материал не найден"));

            mockMvc.perform(get("/api/v1/materials/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/materials/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/materials =====================

    @Nested
    @DisplayName("POST /api/v1/materials")
    class CreateMaterial {

        @Test
        @DisplayName("ADMIN creates material")
        @WithMockUser(roles = "ADMIN")
        void adminCreatesMaterial() throws Exception {
            when(materialService.createMaterial(any(MaterialCreateRequest.class), any()))
                    .thenReturn(materialResponse());

            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Баннер 440 г/м2"));
        }

        @Test
        @DisplayName("GOD creates material")
        @WithMockUser(roles = "GOD")
        void godCreatesMaterial() throws Exception {
            when(materialService.createMaterial(any(MaterialCreateRequest.class), any()))
                    .thenReturn(materialResponse());

            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("MANAGER cannot create material")
        @WithMockUser(roles = "MANAGER")
        void managerCannotCreateMaterial() throws Exception {
            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isForbidden());

            verify(materialService, never()).createMaterial(any(), any());
        }

        @Test
        @DisplayName("PRODUCTION cannot create material")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotCreateMaterial() throws Exception {
            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== PUT /api/v1/materials/{id} =====================

    @Nested
    @DisplayName("PUT /api/v1/materials/{id}")
    class UpdateMaterial {

        @Test
        @DisplayName("ADMIN updates material")
        @WithMockUser(roles = "ADMIN")
        void adminUpdatesMaterial() throws Exception {
            MaterialUpdateRequest request = new MaterialUpdateRequest();
            request.setName("Баннер 510 г/м2");
            request.setPrice(new BigDecimal("180.00"));

            MaterialResponse updated = materialResponse();
            updated.setName("Баннер 510 г/м2");
            when(materialService.updateMaterial(any(), any(MaterialUpdateRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/materials/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Баннер 510 г/м2"));
        }

        @Test
        @DisplayName("Returns 400 for invalid unit")
        @WithMockUser(roles = "ADMIN")
        void returns400ForInvalidUnit() throws Exception {
            MaterialUpdateRequest request = new MaterialUpdateRequest();
            request.setUnit("kg");

            when(materialService.updateMaterial(any(), any(MaterialUpdateRequest.class)))
                    .thenThrow(new IllegalArgumentException("Недопустимая единица измерения"));

            mockMvc.perform(put("/api/v1/materials/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("MANAGER cannot update material")
        @WithMockUser(roles = "MANAGER")
        void managerCannotUpdateMaterial() throws Exception {
            mockMvc.perform(put("/api/v1/materials/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== DELETE /api/v1/materials/{id} =====================

    @Nested
    @DisplayName("DELETE /api/v1/materials/{id}")
    class DeleteMaterial {

        @Test
        @DisplayName("ADMIN deletes material")
        @WithMockUser(roles = "ADMIN")
        void adminDeletesMaterial() throws Exception {
            doNothing().when(materialService).deleteMaterial(1L);

            mockMvc.perform(delete("/api/v1/materials/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("PRODUCTION cannot delete material")
        @WithMockUser(roles = "PRODUCTION")
        void productionCannotDeleteMaterial() throws Exception {
            mockMvc.perform(delete("/api/v1/materials/1"))
                    .andExpect(status().isForbidden());

            verify(materialService, never()).deleteMaterial(any());
        }

        @Test
        @DisplayName("USER cannot delete material")
        @WithMockUser(roles = "USER")
        void userCannotDeleteMaterial() throws Exception {
            mockMvc.perform(delete("/api/v1/materials/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous user gets 401 on GET /api/v1/materials")
        @WithAnonymousUser
        void anonymousGetAll401() throws Exception {
            mockMvc.perform(get("/api/v1/materials"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on POST /api/v1/materials")
        @WithAnonymousUser
        void anonymousCreate401() throws Exception {
            mockMvc.perform(post("/api/v1/materials")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on DELETE /api/v1/materials/{id}")
        @WithAnonymousUser
        void anonymousDelete401() throws Exception {
            mockMvc.perform(delete("/api/v1/materials/1"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
