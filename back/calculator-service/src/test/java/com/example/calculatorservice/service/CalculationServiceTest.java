package com.example.calculatorservice.service;

import com.example.calculatorservice.dto.request.CalculationRequestDto;
import com.example.calculatorservice.dto.response.CalculationResponseDto;
import com.example.calculatorservice.entity.*;
import com.example.calculatorservice.exception.BadRequestException;
import com.example.calculatorservice.mapper.CalculationMapper;
import com.example.calculatorservice.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mapstruct.factory.Mappers;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private EyeletRepository eyeletRepository;
    @Mock private OperationRepository operationRepository;
    @Mock private CalculationRepository calculationRepository;

    private CalculationMapper calculationMapper;

    @BeforeEach
    void setUpMapper() {
        calculationMapper = Mappers.getMapper(CalculationMapper.class);
    }

    private CalculationService calculationService;

    private Material testMaterial;

    @BeforeEach
    void setUp() {
        testMaterial = new Material();
        testMaterial.setId(1L);
        testMaterial.setName("Баннер 510 г/м²");
        testMaterial.setPricePerSquareMeter(new BigDecimal("200.00"));
        testMaterial.setWasteCoefficient(BigDecimal.ONE);
        testMaterial.setDeleted(false);

        calculationService = new CalculationService(
                materialRepository,
                eyeletRepository,
                operationRepository,
                calculationRepository,
                calculationMapper
        );
    }

    // ===================== calculateAreaWithPodvorot =====================

    @Nested
    @DisplayName("calculateAreaWithPodvorot")
    class CalculateAreaWithPodvorot {

        @Test
        @DisplayName("Simple area without podvorot")
        void simpleAreaWithoutPodvorot() {
            BigDecimal area = calculationService.calculateAreaWithPodvorot(
                    new BigDecimal("2.0"),
                    new BigDecimal("3.0"),
                    null, null, 2);
            assertThat(area).isEqualByComparingTo(new BigDecimal("6.0"));
        }

        @Test
        @DisplayName("Area with horizontal podvorot: 20mm per side, 2 folds per side")
        void areaWithHorizontalPodvorot() {
            BigDecimal area = calculationService.calculateAreaWithPodvorot(
                    new BigDecimal("2.0"),
                    new BigDecimal("3.0"),
                    new BigDecimal("20"),   // podvorotMmHorizontal
                    null,                      // no vertical podvorot
                    2);                        // 2 folds per side
            // extraW = 20/1000 * (2*2) = 0.04 * 4 = 0.08
            // effectiveWidth = 2.0 + 0.08 = 2.08
            // area = 2.08 * 3.0 = 6.24
            assertThat(area).isEqualByComparingTo(new BigDecimal("6.24"));
        }

        @Test
        @DisplayName("Area with both horizontal and vertical podvorot")
        void areaWithBothPodvorots() {
            BigDecimal area = calculationService.calculateAreaWithPodvorot(
                    new BigDecimal("2.0"),
                    new BigDecimal("3.0"),
                    new BigDecimal("20"),   // horizontal
                    new BigDecimal("15"),   // vertical
                    2);
            // extraW = 20/1000 * 4 = 0.08, width = 2.08
            // extraH = 15/1000 * 4 = 0.06, height = 3.06
            // area = 2.08 * 3.06 = 6.3648
            assertThat(area).isEqualByComparingTo(new BigDecimal("6.3648"));
        }

        @Test
        @DisplayName("Default podvorotCountPerSide=2 when null")
        void defaultPodvorotCount() {
            BigDecimal area = calculationService.calculateAreaWithPodvorot(
                    new BigDecimal("1.0"),
                    new BigDecimal("1.0"),
                    new BigDecimal("50"), null, null);
            // extraW = 50/1000 * (2*2) = 0.05 * 4 = 0.20
            // width = 1.20, area = 1.20
            assertThat(area).isEqualByComparingTo(new BigDecimal("1.20"));
        }
    }

    // ===================== calculateMaterialArea (via calculateWithoutSaving) =====================

    @Nested
    @DisplayName("Material area calculation (via calculateWithoutSaving)")
    class MaterialAreaCalculation {

        @Test
        @DisplayName("Basic area: 2m x 3m = 6 m²")
        void basicArea() {
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // materialCost = 6.0 * 200.00 * 1.0 = 1200.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1200.00"));
        }

        @Test
        @DisplayName("Area with waste coefficient 1.10")
        void areaWithWasteCoefficient() {
            testMaterial.setWasteCoefficient(new BigDecimal("1.10"));
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // materialCost = 6.0 * 200.00 * 1.10 = 1320.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1320.00"));
        }
    }

    // ===================== Operation calculations =====================

    @Nested
    @DisplayName("Operation quantity calculation")
    class OperationQuantityCalculation {

        @Test
        @DisplayName("SQUARE_METER operation: quantity = material area")
        void squareMeterOperation() {
            Operation op = createOperation(1L, "Печать 720 dpi", UnitType.SQUARE_METER, new BigDecimal("50.00"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(List.of(1L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(1L)).thenReturn(Optional.of(op));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // materialCost = 6.0 * 200 = 1200.00
            // opQty = 6.0 m², opSubtotal = 6.0 * 50.00 = 300.00
            // total = 1200.00 + 300.00 = 1500.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(response.getOperations()).hasSize(1);
            assertThat(response.getOperations().get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("6.0"));
            assertThat(response.getOperations().get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("LINEAR_METER operation: quantity = perimeter")
        void linearMeterOperation() {
            Operation op = createOperation(2L, "Резка", UnitType.LINEAR_METER, new BigDecimal("5.00"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(List.of(2L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(2L)).thenReturn(Optional.of(op));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // materialCost = 6.0 * 200 = 1200.00
            // perimeter = (2.0 + 3.0) * 2 = 10.0
            // opSubtotal = 10.0 * 5.00 = 50.00
            // total = 1200.00 + 50.00 = 1250.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(response.getOperations()).hasSize(1);
            assertThat(response.getOperations().get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("10.0"));
        }

        @Test
        @DisplayName("Multiple operations: SQUARE_METER + LINEAR_METER")
        void multipleOperations() {
            Operation printOp = createOperation(1L, "Печать 720 dpi", UnitType.SQUARE_METER, new BigDecimal("50.00"));
            Operation cutOp = createOperation(2L, "Резка", UnitType.LINEAR_METER, new BigDecimal("5.00"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(List.of(1L, 2L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(1L)).thenReturn(Optional.of(printOp));
            when(operationRepository.findById(2L)).thenReturn(Optional.of(cutOp));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // materialCost = 6.0 * 200 = 1200.00
            // print: 6.0 * 50.00 = 300.00
            // cut: 10.0 * 5.00 = 50.00
            // total = 1200 + 300 + 50 = 1550.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1550.00"));
            assertThat(response.getOperations()).hasSize(2);
        }

        @Test
        @DisplayName("Hem operation with hemWidthMm and hemCount parameters")
        void hemOperationWithParameters() {
            Operation hemOp = createOperation(3L, "Подворот", UnitType.SQUARE_METER, new BigDecimal("10.00"));
            hemOp.setHemWidthMm(20);
            hemOp.setHemCount(2);

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(List.of(3L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(3L)).thenReturn(Optional.of(hemOp));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // hem formula: widthMm = 2000 + 20*2 = 2040, heightMm = 3000 + 20*2 = 3040
            // areaMm2 = 2040 * 3040 = 6201600
            // areaM2 = 6201600 / 1000000 = 6.2016
            // opSubtotal = 6.2016 * 10.00 = 62.16 (rounded)
            assertThat(response.getOperations()).hasSize(1);
            BigDecimal expectedArea = new BigDecimal("2040").multiply(new BigDecimal("3040"))
                    .divide(new BigDecimal("1000000"), 4, RoundingMode.HALF_UP);
            assertThat(response.getOperations().get(0).getQuantity()).isEqualByComparingTo(expectedArea);
        }
    }

    // ===================== Eyelet calculations =====================

    @Nested
    @DisplayName("Eyelet quantity calculation")
    class EyeletCalculation {

        @Test
        @DisplayName("Eyelets along perimeter: 2x3m banner, step 40cm")
        void eyeletsOnBanner() {
            Eyelet eyelet = createEyelet(1L, "Люверс 8мм", new BigDecimal("2.00"), 8);
            Operation eyeletOp = createOperation(10L, "Установка люверсов", UnitType.PIECE, new BigDecimal("0"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setEyeletId(1L);
            request.setEyeletStepCm(40);
            request.setOperationIds(List.of(10L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(eyeletRepository.findById(1L)).thenReturn(Optional.of(eyelet));
            when(operationRepository.findById(10L)).thenReturn(Optional.of(eyeletOp));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // perimeter = (2+3)*2 = 10m = 1000cm
            // eyelets = 1000/40 = 25
            // eyeletCost = 25 * 2.00 = 50.00
            // materialCost = 6.0 * 200 = 1200.00
            // total = 1200 + 50 (eyelet hardware) + 0 (eyelet op) = 1250.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1250.00"));
            assertThat(response.getEyelet().getQuantity()).isEqualByComparingTo(new BigDecimal("25"));
            assertThat(response.getEyelet().getSubtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("Eyelet cost with PIECE-based installation operation")
        void eyeletWithInstallationOp() {
            Eyelet eyelet = createEyelet(1L, "Люверс 8мм", new BigDecimal("3.00"), 8);
            Operation installOp = createOperation(10L, "Установка люверсов", UnitType.PIECE, new BigDecimal("5.00"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("1.0"));
            request.setHeightM(new BigDecimal("1.0"));
            request.setEyeletId(1L);
            request.setEyeletStepCm(40);
            request.setOperationIds(List.of(10L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(eyeletRepository.findById(1L)).thenReturn(Optional.of(eyelet));
            when(operationRepository.findById(10L)).thenReturn(Optional.of(installOp));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // perimeter = 4m = 400cm, eyelets = 400/40 = 10
            // eyelet hardware = 10 * 3.00 = 30.00
            // install op = 10 * 5.00 = 50.00
            // materialCost = 1.0 * 200 = 200.00
            // total = 200 + 30 + 50 = 280.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("280.00"));
        }
    }

    // ===================== Validation =====================

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Throws when width is zero")
        void throwsWhenWidthZero() {
            CalculationRequestDto request = baseRequest();
            request.setWidthM(BigDecimal.ZERO);
            request.setHeightM(new BigDecimal("1.0"));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Ширина");
        }

        @Test
        @DisplayName("Throws when width is negative")
        void throwsWhenWidthNegative() {
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("-1.0"));
            request.setHeightM(new BigDecimal("1.0"));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Throws when material not found")
        void throwsWhenMaterialNotFound() {
            CalculationRequestDto request = baseRequest();
            request.setMaterialId(99L);
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Материал не найден");
        }

        @Test
        @DisplayName("Throws when operation not found")
        void throwsWhenOperationNotFound() {
            CalculationRequestDto request = baseRequest();
            request.setOperationIds(List.of(999L));

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Операция с ID 999");
        }

        @Test
        @DisplayName("Throws when eyeletStepCm is zero")
        void throwsWhenEyeletStepZero() {
            CalculationRequestDto request = baseRequest();
            request.setEyeletStepCm(0);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Шаг люверсов");
        }

        @Test
        @DisplayName("Throws when podvorotCountPerSide < 1")
        void throwsWhenPodvorotCountLessThanOne() {
            CalculationRequestDto request = baseRequest();
            request.setPodvorotMmHorizontal(new BigDecimal("10"));
            request.setPodvorotCountPerSide(0);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            assertThatThrownBy(() -> calculationService.calculateWithoutSaving(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Количество подворотов");
        }
    }

    // ===================== Edge cases =====================

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Default waste coefficient = 1.0 when null")
        void defaultWasteCoefficient() {
            testMaterial.setWasteCoefficient(null);
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("1.0"));
            request.setHeightM(new BigDecimal("1.0"));
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Default eyeletStepCm = 40 when null")
        void defaultEyeletStep() {
            Eyelet eyelet = createEyelet(1L, "Люверс 8мм", new BigDecimal("2.00"), 8);

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("1.0"));
            request.setHeightM(new BigDecimal("1.0"));
            request.setEyeletId(1L);
            request.setEyeletStepCm(null);
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(eyeletRepository.findById(1L)).thenReturn(Optional.of(eyelet));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // perimeter = 4m, step=40cm -> 400/40 = 10 eyelets
            assertThat(response.getEyelet().getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("Perimeter rounding: 3.33m perimeter / 40cm = 8.325 -> rounds UP to 9")
        void eyeletRoundingUp() {
            Eyelet eyelet = createEyelet(1L, "Люверс", new BigDecimal("1.00"), 8);

            CalculationRequestDto request = baseRequest();
            // 0.83 x 1.0: perimeter = 3.66m = 366cm, 366/40 = 9.15 -> 10
            request.setWidthM(new BigDecimal("0.83"));
            request.setHeightM(new BigDecimal("1.0"));
            request.setEyeletId(1L);
            request.setEyeletStepCm(40);
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));
            when(eyeletRepository.findById(1L)).thenReturn(Optional.of(eyelet));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // perimeter = (0.83 + 1.0) * 2 = 3.66m = 366cm
            // 366 / 40 = 9.15 -> round UP = 10
            assertThat(response.getEyelet().getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("Height defaults to 1.0 when null (for linear materials)")
        void heightDefaultsToOne() {
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("5.0"));
            request.setHeightM(null);
            request.setOperationIds(null);

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);
            // area = 5.0 * 1.0 = 5.0, cost = 5.0 * 200 = 1000.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1000.00"));
        }

        @Test
        @DisplayName("Empty operation list works correctly")
        void emptyOperationList() {
            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("2.0"));
            request.setHeightM(new BigDecimal("3.0"));
            request.setOperationIds(List.of());

            when(materialRepository.findByIdAndDeletedFalse(1L))
                    .thenReturn(Optional.of(testMaterial));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(response.getOperations()).isEmpty();
        }
    }

    // ===================== Full integration-like calculation =====================

    @Nested
    @DisplayName("Full calculation scenarios")
    class FullCalculationScenarios {

        @Test
        @DisplayName("Complete banner order: material + print + cut + hem + eyelets")
        void completeBannerOrder() {
            testMaterial.setWasteCoefficient(new BigDecimal("1.05"));

            Material savedMaterial = new Material();
            savedMaterial.setId(1L);
            savedMaterial.setName("Баннер 510 г/м²");
            savedMaterial.setPricePerSquareMeter(new BigDecimal("250.00"));
            savedMaterial.setWasteCoefficient(new BigDecimal("1.05"));
            savedMaterial.setDeleted(false);

            Operation printOp = createOperation(1L, "Печать 720 dpi", UnitType.SQUARE_METER, new BigDecimal("80.00"));
            Operation cutOp = createOperation(2L, "Резка", UnitType.LINEAR_METER, new BigDecimal("8.00"));
            Operation hemOp = createOperation(3L, "Подворот", UnitType.SQUARE_METER, new BigDecimal("15.00"));
            hemOp.setHemWidthMm(25);
            hemOp.setHemCount(2);

            Eyelet eyelet = createEyelet(1L, "Люверс 10мм", new BigDecimal("3.50"), 10);
            Operation eyeletOp = createOperation(4L, "Установка люверсов", UnitType.PIECE, new BigDecimal("7.00"));

            CalculationRequestDto request = new CalculationRequestDto();
            request.setMaterialId(1L);
            request.setWidthM(new BigDecimal("3.0"));
            request.setHeightM(new BigDecimal("2.0"));
            request.setEyeletId(1L);
            request.setEyeletStepCm(30);
            request.setOperationIds(List.of(1L, 2L, 3L, 4L));

            when(materialRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(savedMaterial));
            when(operationRepository.findById(1L)).thenReturn(Optional.of(printOp));
            when(operationRepository.findById(2L)).thenReturn(Optional.of(cutOp));
            when(operationRepository.findById(3L)).thenReturn(Optional.of(hemOp));
            when(operationRepository.findById(4L)).thenReturn(Optional.of(eyeletOp));
            when(eyeletRepository.findById(1L)).thenReturn(Optional.of(eyelet));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // Manual calculation:
            // area = 3.0 * 2.0 = 6.0 m²
            // materialCost = 6.0 * 250.00 * 1.05 = 1575.00
            // print: 6.0 * 80.00 = 480.00
            // cut: perimeter = (3+2)*2 = 10.0 * 8.00 = 80.00
            // hem: widthMm=3000+50=3050, heightMm=2000+50=2050, areaMm2=3050*2050=6252500, areaM2=6.2525 * 15 = 93.79
            // eyelets: perimeter = 10m = 1000cm / 30 = 33.33 -> 34
            // eyelet hardware: 34 * 3.50 = 119.00
            // eyelet op: 34 * 7.00 = 238.00
            // total = 1575 + 480 + 80 + 93.79 + 119 + 238 = 2585.79

            assertThat(response.getTotalPrice()).isNotNull();
            assertThat(response.getTotalPrice().compareTo(BigDecimal.ZERO)).isGreaterThan(0);
            assertThat(response.getOperations()).hasSize(4);
            assertThat(response.getEyelet()).isNotNull();
        }

        @Test
        @DisplayName("Small banner 0.5x0.5m with waste 1.20")
        void smallBannerWithWaste() {
            testMaterial.setWasteCoefficient(new BigDecimal("1.20"));
            Operation op = createOperation(1L, "Печать", UnitType.SQUARE_METER, new BigDecimal("100.00"));

            CalculationRequestDto request = baseRequest();
            request.setWidthM(new BigDecimal("0.5"));
            request.setHeightM(new BigDecimal("0.5"));
            request.setOperationIds(List.of(1L));

            when(materialRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(testMaterial));
            when(operationRepository.findById(1L)).thenReturn(Optional.of(op));

            CalculationResponseDto response = calculationService.calculateWithoutSaving(request);

            // area = 0.25 m²
            // materialCost = 0.25 * 200 * 1.20 = 60.00
            // print: 0.25 * 100 = 25.00
            // total = 85.00
            assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("85.00"));
        }
    }

    // ===================== Constants / default values =====================

    @Nested
    @DisplayName("Default parameter values")
    class DefaultValues {

        @Test
        @DisplayName("Waste coefficient defaults to 1.0 in entity")
        void wasteCoefficientDefault() {
            Material mat = new Material();
            assertThat(mat.getWasteCoefficient()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("Deleted defaults to false in entity")
        void deletedDefault() {
            Material mat = new Material();
            assertThat(mat.getDeleted()).isFalse();
        }
    }

    // ===================== Helper methods =====================

    private CalculationRequestDto baseRequest() {
        CalculationRequestDto request = new CalculationRequestDto();
        request.setMaterialId(1L);
        request.setWidthM(new BigDecimal("1.0"));
        request.setHeightM(new BigDecimal("1.0"));
        return request;
    }

    private Operation createOperation(Long id, String name, UnitType unit, BigDecimal price) {
        Operation op = new Operation();
        op.setId(id);
        op.setName(name);
        op.setUnit(unit);
        op.setPrice(price);
        op.setDeleted(false);
        return op;
    }

    private Eyelet createEyelet(Long id, String name, BigDecimal pricePerPiece, int diameterMm) {
        Eyelet eyelet = new Eyelet();
        eyelet.setId(id);
        eyelet.setName(name);
        eyelet.setPricePerPiece(pricePerPiece);
        eyelet.setDiameterMm(diameterMm);
        eyelet.setDeleted(false);
        return eyelet;
    }
}
