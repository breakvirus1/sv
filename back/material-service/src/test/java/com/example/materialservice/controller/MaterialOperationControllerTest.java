package com.example.materialservice.controller;

import com.example.materialservice.config.SecurityConfig;
import com.example.materialservice.dto.MaterialOperationCreateRequest;
import com.example.materialservice.dto.MaterialOperationResponse;
import com.example.materialservice.dto.MaterialOperationUpdateRequest;
import com.example.materialservice.dto.OperationCalculateRequest;
import com.example.materialservice.exception.NotFoundException;
import com.example.materialservice.service.MaterialOperationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaterialOperationController.class)
@Import({SecurityConfig.class, MaterialOperationControllerTest.AccessDeniedTestAdvice.class})
@TestPropertySource(properties = {
    "eureka.client.enabled=false"
})
class MaterialOperationControllerTest {

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
    private MaterialOperationService operationService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private MaterialOperationResponse operationResponse() {
        MaterialOperationResponse response = new MaterialOperationResponse();
        response.setId(1L);
        response.setMaterialId(1L);
        response.setMaterialName("Баннер 440 г/м2");
        response.setName("Ламинация");
        response.setOperationType("SQUARE_METER");
        response.setBasePrice(new BigDecimal("40.00"));
        response.setUnit("м2");
        response.setActive(true);
        return response;
    }

    private MaterialOperationCreateRequest createRequest() {
        MaterialOperationCreateRequest request = new MaterialOperationCreateRequest();
        request.setName("Ламинация");
        request.setOperationType("SQUARE_METER");
        request.setBasePrice(new BigDecimal("40.00"));
        return request;
    }

    private MaterialOperationUpdateRequest updateRequest() {
        MaterialOperationUpdateRequest request = new MaterialOperationUpdateRequest();
        request.setName("Ламинация премиум");
        request.setOperationType("SQUARE_METER");
        request.setBasePrice(new BigDecimal("50.00"));
        return request;
    }

    // ===================== GET /api/v1/materials/{materialId}/operations =====================

    @Nested
    @DisplayName("GET /api/v1/materials/{materialId}/operations")
    class GetOperations {

        @Test
        @DisplayName("Authenticated USER gets operations of material")
        @WithMockUser(roles = "USER")
        void userGetsOperations() throws Exception {
            when(operationService.getOperationsByMaterialId(1L)).thenReturn(List.of(operationResponse()));

            mockMvc.perform(get("/api/v1/materials/1/operations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Ламинация"))
                    .andExpect(jsonPath("$[0].basePrice").value(40.00));
        }

        @Test
        @DisplayName("ADMIN gets operations of material")
        @WithMockUser(roles = "ADMIN")
        void adminGetsOperations() throws Exception {
            when(operationService.getOperationsByMaterialId(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/materials/1/operations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Returns 404 for non-existent material")
        @WithMockUser(roles = "MANAGER")
        void returns404ForNonExistent() throws Exception {
            when(operationService.getOperationsByMaterialId(999L))
                    .thenThrow(new NotFoundException("Материал не найден"));

            mockMvc.perform(get("/api/v1/materials/999/operations"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /api/v1/materials/{materialId}/operations =====================

    @Nested
    @DisplayName("POST /api/v1/materials/{materialId}/operations")
    class CreateOperation {

        @Test
        @DisplayName("Authenticated USER creates operation")
        @WithMockUser(roles = "USER")
        void userCreatesOperation() throws Exception {
            when(operationService.createOperation(any(), any(MaterialOperationCreateRequest.class)))
                    .thenReturn(operationResponse());

            mockMvc.perform(post("/api/v1/materials/1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Ламинация"));
        }

        @Test
        @DisplayName("Returns 400 when service rejects invalid input")
        @WithMockUser(roles = "MANAGER")
        void returns400ForInvalidInput() throws Exception {
            when(operationService.createOperation(any(), any(MaterialOperationCreateRequest.class)))
                    .thenThrow(new IllegalArgumentException("Неизвестный тип операции"));

            mockMvc.perform(post("/api/v1/materials/1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Returns 404 when material does not exist")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistentMaterial() throws Exception {
            when(operationService.createOperation(any(), any(MaterialOperationCreateRequest.class)))
                    .thenThrow(new NotFoundException("Материал не найден"));

            mockMvc.perform(post("/api/v1/materials/999/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== PUT /operations/{operationId} =====================

    @Nested
    @DisplayName("PUT /api/v1/materials/{materialId}/operations/{operationId}")
    class UpdateOperation {

        @Test
        @DisplayName("PRODUCTION updates operation")
        @WithMockUser(roles = "PRODUCTION")
        void productionUpdatesOperation() throws Exception {
            MaterialOperationResponse updated = operationResponse();
            updated.setName("Ламинация премиум");
            when(operationService.updateOperation(any(), any(), any(MaterialOperationUpdateRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/api/v1/materials/1/operations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Ламинация премиум"));
        }

        @Test
        @DisplayName("Returns 404 for non-existent operation")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistentOperation() throws Exception {
            when(operationService.updateOperation(any(), any(), any(MaterialOperationUpdateRequest.class)))
                    .thenThrow(new NotFoundException("Операция не найдена"));

            mockMvc.perform(put("/api/v1/materials/1/operations/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest())))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== DELETE /operations/{operationId} =====================

    @Nested
    @DisplayName("DELETE /api/v1/materials/{materialId}/operations/{operationId}")
    class DeleteOperation {

        @Test
        @DisplayName("MANAGER deletes operation")
        @WithMockUser(roles = "MANAGER")
        void managerDeletesOperation() throws Exception {
            doNothing().when(operationService).deleteOperation(1L, 1L);

            mockMvc.perform(delete("/api/v1/materials/1/operations/1"))
                    .andExpect(status().isNoContent());

            verify(operationService).deleteOperation(1L, 1L);
        }

        @Test
        @DisplayName("Returns 404 for non-existent operation")
        @WithMockUser(roles = "ADMIN")
        void returns404ForNonExistentOperation() throws Exception {
            doThrow(new NotFoundException("Операция не найдена"))
                    .when(operationService).deleteOperation(1L, 999L);

            mockMvc.perform(delete("/api/v1/materials/1/operations/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===================== POST /operations/{operationId}/calculate-quantity =====================

    @Nested
    @DisplayName("POST /api/v1/materials/{materialId}/operations/{operationId}/calculate-quantity")
    class CalculateQuantity {

        @Test
        @DisplayName("USER gets calculated quantity")
        @WithMockUser(roles = "USER")
        void userGetsQuantity() throws Exception {
            OperationCalculateRequest request = new OperationCalculateRequest(
                    new BigDecimal("3.00"), new BigDecimal("2.00"), null, null);

            when(operationService.calculateQuantity(any(), any(OperationCalculateRequest.class)))
                    .thenReturn(new BigDecimal("24.00"));

            mockMvc.perform(post("/api/v1/materials/1/operations/1/calculate-quantity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(24.00));
        }
    }

    // ===================== Authentication =====================

    @Nested
    @DisplayName("Authentication")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous user gets 401 on GET operations")
        @WithAnonymousUser
        void anonymousGet401() throws Exception {
            mockMvc.perform(get("/api/v1/materials/1/operations"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on POST operations")
        @WithAnonymousUser
        void anonymousCreate401() throws Exception {
            mockMvc.perform(post("/api/v1/materials/1/operations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on calculate-quantity")
        @WithAnonymousUser
        void anonymousCalculate401() throws Exception {
            mockMvc.perform(post("/api/v1/materials/1/operations/1/calculate-quantity")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
