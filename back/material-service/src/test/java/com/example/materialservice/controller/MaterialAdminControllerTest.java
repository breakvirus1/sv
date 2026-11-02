package com.example.materialservice.controller;

import com.example.materialservice.config.SecurityConfig;
import com.example.materialservice.entity.Material;
import com.example.materialservice.entity.MaterialType;
import com.example.materialservice.service.MaterialAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpStatus;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialAdminController.class)
@Import({SecurityConfig.class, MaterialAdminControllerTest.AccessDeniedTestAdvice.class})
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class MaterialAdminControllerTest {

    @ControllerAdvice
    static class AccessDeniedTestAdvice {

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<Void> handleAccessDenied(AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialAdminService materialAdminService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Material testMaterial(Long id, String name) {
        Material material = new Material();
        material.setId(id);
        material.setName(name);
        material.setType(MaterialType.MATERIAL);
        material.setUnit("м2");
        material.setPrice(new BigDecimal("150.00"));
        material.setWasteCoefficient(BigDecimal.ONE);
        return material;
    }

    // ===================== POST /api/v1/admin/materials/generate =====================

    @Nested
    @DisplayName("POST /api/v1/admin/materials/generate")
    class GenerateTestMaterials {

        @Test
        @DisplayName("ADMIN generates 20 test materials")
        @WithMockUser(roles = "ADMIN")
        void adminGeneratesMaterials() throws Exception {
            when(materialAdminService.generateTestMaterials(20))
                    .thenReturn(List.of(testMaterial(1L, "Баннер 440 г/м2"), testMaterial(2L, "Плёнка 80 мкм")));

            mockMvc.perform(post("/api/v1/admin/materials/generate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Баннер 440 г/м2"))
                    .andExpect(jsonPath("$[1].name").value("Плёнка 80 мкм"));
        }

        @Test
        @DisplayName("GOD cannot generate test materials")
        @WithMockUser(roles = "GOD")
        void godCannotGenerateMaterials() throws Exception {
            mockMvc.perform(post("/api/v1/admin/materials/generate"))
                    .andExpect(status().isForbidden());

            verify(materialAdminService, never()).generateTestMaterials(anyInt());
        }

        @Test
        @DisplayName("MANAGER cannot generate test materials")
        @WithMockUser(roles = "MANAGER")
        void managerCannotGenerateMaterials() throws Exception {
            mockMvc.perform(post("/api/v1/admin/materials/generate"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("USER cannot generate test materials")
        @WithMockUser(roles = "USER")
        void userCannotGenerateMaterials() throws Exception {
            mockMvc.perform(post("/api/v1/admin/materials/generate"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous user gets 401")
        @WithAnonymousUser
        void anonymousGets401() throws Exception {
            mockMvc.perform(post("/api/v1/admin/materials/generate"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET on admin path is not allowed for anonymous user")
        @WithAnonymousUser
        void anonymousGet401() throws Exception {
            mockMvc.perform(get("/api/v1/admin/materials/generate"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
