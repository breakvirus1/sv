package com.example.calculatorservice.controller;

import com.example.calculatorservice.config.SecurityConfig;
import com.example.calculatorservice.dto.GroupedOperationsResponse;
import com.example.calculatorservice.dto.MaterialDto;
import com.example.calculatorservice.dto.OperationDto;
import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.request.AreaCalculationRequest;
import com.example.calculatorservice.dto.request.ItemCostRequest;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.entity.Eyelet;
import com.example.calculatorservice.entity.Material;
import com.example.calculatorservice.entity.MaterialType;
import com.example.calculatorservice.entity.Operation;
import com.example.calculatorservice.exception.ResourceNotFoundException;
import com.example.calculatorservice.service.CalculationService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalculationController.class)
@Import({SecurityConfig.class, CalculationControllerTest.AccessDeniedTestAdvice.class})
@TestPropertySource(properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=none",
    "eureka.client.enabled=false"
})
class CalculationControllerTest {

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
    private CalculationService calculationService;

    @MockBean
    private com.example.calculatorservice.service.MaterialService materialService;

    @MockBean
    private com.example.calculatorservice.service.OperationService operationService;

    @MockBean
    private com.example.calculatorservice.service.EyeletService eyeletService;

    @MockBean
    private com.example.calculatorservice.mapper.OperationMapper operationMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private CalculationRequestDto validCalculationRequest() {
        CalculationRequestDto request = new CalculationRequestDto();
        request.setMaterialId(1L);
        request.setMaterialType("BANNER");
        request.setWidthM(new BigDecimal("2.00"));
        request.setHeightM(new BigDecimal("1.00"));
        request.setOperationIds(List.of(1L, 2L));
        return request;
    }

    private CalculationResponseDto calculationResponse() {
        CalculationResponseDto response = new CalculationResponseDto();
        response.setId(1L);
        response.setMaterialName("Баннер 440 г/м2");
        response.setWidthM(new BigDecimal("2.00"));
        response.setHeightM(new BigDecimal("1.00"));
        response.setTotalPrice(new BigDecimal("1234.50"));
        response.setOperations(List.of());
        return response;
    }

    // ===================== GET /api/v1/calculations/materials =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/materials")
    class GetAllMaterials {

        @Test
        @DisplayName("ADMIN gets list of materials")
        @WithMockUser(roles = "ADMIN")
        void adminGetsMaterials() throws Exception {
            Material material = new Material();
            material.setId(1L);
            material.setName("Баннер 440 г/м2");

            MaterialDto dto = new MaterialDto();
            dto.setId(1L);
            dto.setName("Баннер 440 г/м2");
            dto.setPricePerSquareMeter(new BigDecimal("150.00"));

            when(materialService.getAllMaterials()).thenReturn(List.of(material));
            when(operationMapper.toMaterialDto(any(Material.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/calculations/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Баннер 440 г/м2"))
                    .andExpect(jsonPath("$[0].pricePerSquareMeter").value(150.00));
        }

        @Test
        @DisplayName("MANAGER gets list of materials")
        @WithMockUser(roles = "MANAGER")
        void managerGetsMaterials() throws Exception {
            when(materialService.getAllMaterials()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/calculations/materials"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT gets 403")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/calculations/materials/{type} =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/materials/{type}")
    class GetMaterialsByType {

        @Test
        @DisplayName("PRODUCTION gets materials of valid type")
        @WithMockUser(roles = "PRODUCTION")
        void productionGetsMaterialsByType() throws Exception {
            MaterialDto dto = new MaterialDto();
            dto.setId(2L);
            dto.setName("Плёнка 80 мкм");
            dto.setType("PLENKA");

            when(materialService.getMaterialsByType(MaterialType.PLENKA))
                    .thenReturn(List.of(new Material()));
            when(operationMapper.toMaterialDto(any(Material.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/calculations/materials/PLENKA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Плёнка 80 мкм"));
        }

        @Test
        @DisplayName("Unknown material type returns 400")
        @WithMockUser(roles = "ADMIN")
        void unknownTypeReturns400() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials/UNKNOWN_TYPE"))
                    .andExpect(status().isBadRequest());

            verify(materialService, never()).getMaterialsByType(any());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials/BANNER"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/calculations/eyelets =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/eyelets")
    class GetAllEyelets {

        @Test
        @DisplayName("GOD gets eyelets")
        @WithMockUser(roles = "GOD")
        void godGetsEyelets() throws Exception {
            Eyelet eyelet = new Eyelet();
            eyelet.setId(1L);
            eyelet.setName("Люверс 12 мм");
            eyelet.setPricePerPiece(new BigDecimal("3.50"));
            eyelet.setDiameterMm(12);

            when(eyeletService.getAllEyelets()).thenReturn(List.of(eyelet));

            mockMvc.perform(get("/api/v1/calculations/eyelets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Люверс 12 мм"))
                    .andExpect(jsonPath("$[0].diameterMm").value(12));
        }

        @Test
        @DisplayName("ACCOUNTANT gets 403")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/eyelets"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/calculations/operations =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/operations")
    class GetAllOperations {

        @Test
        @DisplayName("MANAGER gets operations")
        @WithMockUser(roles = "MANAGER")
        void managerGetsOperations() throws Exception {
            OperationDto dto = new OperationDto();
            dto.setId(1L);
            dto.setName("Ламинация");
            dto.setPrice(new BigDecimal("40.00"));

            when(operationService.getAllOperations()).thenReturn(List.of(new Operation()));
            when(operationMapper.toDto(any(Operation.class))).thenReturn(dto);

            mockMvc.perform(get("/api/v1/calculations/operations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Ламинация"))
                    .andExpect(jsonPath("$[0].price").value(40.00));
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/operations"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/calculations =====================

    @Nested
    @DisplayName("POST /api/v1/calculations")
    class CreateCalculation {

        @Test
        @DisplayName("ADMIN creates calculation")
        @WithMockUser(roles = "ADMIN")
        void adminCreatesCalculation() throws Exception {
            when(calculationService.createAndCalculate(any(CalculationRequestDto.class)))
                    .thenReturn(calculationResponse());

            mockMvc.perform(post("/api/v1/calculations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.materialName").value("Баннер 440 г/м2"))
                    .andExpect(jsonPath("$.totalPrice").value(1234.50));
        }

        @Test
        @DisplayName("Returns 400 when materialId is missing")
        @WithMockUser(roles = "ADMIN")
        void returns400WhenMaterialIdMissing() throws Exception {
            mockMvc.perform(post("/api/v1/calculations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(calculationService, never()).createAndCalculate(any());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(post("/api/v1/calculations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /api/v1/calculations/{id} =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/{id}")
    class GetCalculation {

        @Test
        @DisplayName("ADMIN gets calculation by id")
        @WithMockUser(roles = "ADMIN")
        void adminGetsCalculation() throws Exception {
            when(calculationService.getById(1L)).thenReturn(calculationResponse());

            mockMvc.perform(get("/api/v1/calculations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.totalPrice").value(1234.50));
        }

        @Test
        @DisplayName("Returns 404 for non-existent calculation")
        @WithMockUser(roles = "MANAGER")
        void returns404ForNonExistent() throws Exception {
            when(calculationService.getById(999L))
                    .thenThrow(new ResourceNotFoundException("Расчёт не найден"));

            mockMvc.perform(get("/api/v1/calculations/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/1"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/calculations/calculate-area =====================

    @Nested
    @DisplayName("POST /api/v1/calculations/calculate-area")
    class CalculateArea {

        private AreaCalculationRequest validRequest() {
            AreaCalculationRequest request = new AreaCalculationRequest();
            request.setWidthM(new BigDecimal("3.00"));
            request.setHeightM(new BigDecimal("2.00"));
            request.setPodvorotMmHorizontal(new BigDecimal("20"));
            request.setPodvorotMmVertical(new BigDecimal("20"));
            request.setPodvorotCountPerSide(2);
            return request;
        }

        @Test
        @DisplayName("ADMIN gets calculated area")
        @WithMockUser(roles = "ADMIN")
        void adminGetsArea() throws Exception {
            when(calculationService.calculateAreaWithPodvorot(any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("6.80"));

            mockMvc.perform(post("/api/v1/calculations/calculate-area")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(6.80));
        }

        @Test
        @DisplayName("Returns 400 when width is missing")
        @WithMockUser(roles = "ADMIN")
        void returns400WhenWidthMissing() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/calculate-area")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/calculate-area")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/calculations/estimate-item =====================

    @Nested
    @DisplayName("POST /api/v1/calculations/estimate-item")
    class EstimateItem {

        private ItemCostRequest validRequest() {
            ItemCostRequest request = new ItemCostRequest();
            request.setMaterialId(1L);
            request.setWidthM(new BigDecimal("2.00"));
            request.setHeightM(new BigDecimal("1.00"));
            request.setPodvorotCountPerSide(2);
            request.setOperationIds(List.of(1L));
            return request;
        }

        @Test
        @DisplayName("MANAGER gets estimated cost")
        @WithMockUser(roles = "MANAGER")
        void managerGetsEstimate() throws Exception {
            when(calculationService.calculateItemCost(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("980.00"));

            mockMvc.perform(post("/api/v1/calculations/estimate-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(980.00));
        }

        @Test
        @DisplayName("Returns 400 when materialId is missing")
        @WithMockUser(roles = "ADMIN")
        void returns400WhenMaterialIdMissing() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/estimate-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"widthM\":1.0,\"heightM\":1.0}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ACCOUNTANT gets 403")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantGets403() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/estimate-item")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== POST /api/v1/calculations/preview =====================

    @Nested
    @DisplayName("POST /api/v1/calculations/preview")
    class Preview {

        @Test
        @DisplayName("ADMIN previews calculation without saving")
        @WithMockUser(roles = "ADMIN")
        void adminPreviewsCalculation() throws Exception {
            CalculationResponseDto response = calculationResponse();
            response.setId(null);
            response.setTotalPrice(new BigDecimal("500.00"));

            when(calculationService.calculateWithoutSaving(any(CalculationRequestDto.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalPrice").value(500.00))
                    .andExpect(jsonPath("$.materialName").value("Баннер 440 г/м2"));
        }

        @Test
        @DisplayName("GOD previews calculation without saving")
        @WithMockUser(roles = "GOD")
        void godPreviewsCalculation() throws Exception {
            when(calculationService.calculateWithoutSaving(any(CalculationRequestDto.class)))
                    .thenReturn(calculationResponse());

            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @DisplayName("Returns 400 when materialId is missing")
        @WithMockUser(roles = "ADMIN")
        void returns400WhenMaterialIdMissing() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());

            verify(calculationService, never()).calculateWithoutSaving(any());
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ACCOUNTANT gets 403")
        @WithMockUser(roles = "ACCOUNTANT")
        void accountantGets403() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCalculationRequest())))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /materials/{materialId}/grouped-operations =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/materials/{materialId}/grouped-operations")
    class GetGroupedOperationsByMaterial {

        @Test
        @DisplayName("ADMIN gets grouped operations for material")
        @WithMockUser(roles = "ADMIN")
        void adminGetsGroupedOperations() throws Exception {
            OperationDto opDto = new OperationDto();
            opDto.setId(1L);
            opDto.setName("Подгиб");

            GroupedOperationsResponse response = new GroupedOperationsResponse(
                    List.of(new GroupedOperationsResponse.GroupDto(1L, "Подвороты", List.of(opDto))),
                    List.of()
            );
            when(operationService.getGroupedOperationsByMaterialId(1L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/calculations/materials/1/grouped-operations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groups[0].name").value("Подвороты"))
                    .andExpect(jsonPath("$.groups[0].operations[0].name").value("Подгиб"))
                    .andExpect(jsonPath("$.ungroupedOperations.length()").value(0));
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials/1/grouped-operations"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== GET /operations/grouped =====================

    @Nested
    @DisplayName("GET /api/v1/calculations/operations/grouped")
    class GetAllGroupedOperations {

        @Test
        @DisplayName("MANAGER gets all grouped operations without material filter")
        @WithMockUser(roles = "MANAGER")
        void managerGetsAllGroupedOperations() throws Exception {
            GroupedOperationsResponse response = new GroupedOperationsResponse(List.of(), List.of());
            when(operationService.getGroupedOperationsByMaterialId(null)).thenReturn(response);

            mockMvc.perform(get("/api/v1/calculations/operations/grouped"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groups.length()").value(0))
                    .andExpect(jsonPath("$.ungroupedOperations.length()").value(0));
        }

        @Test
        @DisplayName("PRODUCTION gets grouped operations filtered by material")
        @WithMockUser(roles = "PRODUCTION")
        void productionGetsFilteredGroupedOperations() throws Exception {
            GroupedOperationsResponse response = new GroupedOperationsResponse(List.of(), List.of());
            when(operationService.getGroupedOperationsByMaterialId(5L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/calculations/operations/grouped")
                            .param("materialId", "5"))
                    .andExpect(status().isOk());

            verify(operationService).getGroupedOperationsByMaterialId(5L);
        }

        @Test
        @DisplayName("USER gets 403")
        @WithMockUser(roles = "USER")
        void userGets403() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/operations/grouped"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===================== Authentication / Authorization =====================

    @Nested
    @DisplayName("Authentication and authorization")
    class AuthChecks {

        @Test
        @DisplayName("Anonymous user gets 401 on GET /materials")
        @WithAnonymousUser
        void anonymousGetMaterials401() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/materials"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on GET /{id}")
        @WithAnonymousUser
        void anonymousGetCalculation401() throws Exception {
            mockMvc.perform(get("/api/v1/calculations/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on POST /preview")
        @WithAnonymousUser
        void anonymousPreview401() throws Exception {
            mockMvc.perform(post("/api/v1/calculations/preview")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Anonymous user gets 401 on POST /api/v1/calculations")
        @WithAnonymousUser
        void anonymousCreate401() throws Exception {
            mockMvc.perform(post("/api/v1/calculations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
