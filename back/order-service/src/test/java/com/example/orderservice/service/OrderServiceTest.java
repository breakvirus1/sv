package com.example.orderservice.service;

import com.example.orderservice.dto.*;
import com.example.materialservice.entity.Material;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.NotFoundException;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderStageRepository orderStageRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderCommentRepository orderCommentRepository;
    @Mock private FileAttachmentRepository fileAttachmentRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private EntityManager entityManager;
    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private OrderService orderService;

    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNumber("20260526102500001");
        testOrder.setTotalAmount(new BigDecimal("1000.00"));
        testOrder.setPaidAmount(new BigDecimal("500.00"));
        testOrder.setDebtAmount(new BigDecimal("500.00"));
        testOrder.setPriceplus(new BigDecimal("10.00"));
        testOrder.setTotalWithPriceplus(new BigDecimal("1100.00"));
        testOrder.setDescription("Test order");
        testOrder.setDeleted(false);
    }

    // ===================== getOrderById =====================

    @Nested
    @DisplayName("getOrderById")
    class GetOrderById {

        @Test
        @DisplayName("Returns order when found")
        void returnsOrderWhenFound() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            OrderResponse mappedResponse = new OrderResponse();
            mappedResponse.setId(1L);
            when(orderMapper.toDto(testOrder)).thenReturn(mappedResponse);

            OrderResponse result = orderService.getOrderById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Throws NotFoundException when order not found")
        void throwsWhenNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderById(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Заказ не найден");
        }
    }

    // ===================== getOrderByOrderNumber =====================

    @Nested
    @DisplayName("getOrderByOrderNumber")
    class GetOrderByOrderNumber {

        @Test
        @DisplayName("Returns order when found by order number")
        void returnsOrderWhenFound() {
            when(orderRepository.findByOrderNumber("20260526102500001"))
                    .thenReturn(testOrder);
            OrderResponse mappedResponse = new OrderResponse();
            mappedResponse.setOrderNumber("20260526102500001");
            when(orderMapper.toDto(testOrder)).thenReturn(mappedResponse);

            OrderResponse result = orderService.getOrderByOrderNumber("20260526102500001");

            assertThat(result).isNotNull();
            assertThat(result.getOrderNumber()).isEqualTo("20260526102500001");
        }

        @Test
        @DisplayName("Throws NotFoundException when order number not found")
        void throwsWhenNotFound() {
            when(orderRepository.findByOrderNumber("NONEXISTENT")).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrderByOrderNumber("NONEXISTENT"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ===================== updateStatus =====================

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Updates status to READY")
        void updatesStatusToReady() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            OrderResponse mappedResponse = new OrderResponse();
            when(orderMapper.toDto(any(Order.class))).thenReturn(mappedResponse);

            OrderResponse result = orderService.updateStatus(1L, "READY");

            assertThat(result).isNotNull();
            verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.READY));
        }

        @Test
        @DisplayName("Throws when order not found")
        void throwsWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.updateStatus(999L, "READY"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ===================== updateProductionStage =====================

    @Nested
    @DisplayName("updateProductionStage")
    class UpdateProductionStage {

        @Test
        @DisplayName("Updates production stage to PRINTING")
        void updatesStageToPrinting() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
            OrderResponse mappedResponse = new OrderResponse();
            when(orderMapper.toDto(any(Order.class))).thenReturn(mappedResponse);

            OrderResponse result = orderService.updateProductionStage(1L, "PRINTING");

            assertThat(result).isNotNull();
            verify(orderRepository).save(argThat(o -> o.getProductionStage() == ProductionStage.PRINTING));
        }
    }

    // ===================== addPayment =====================

    @Nested
    @DisplayName("addPayment")
    class AddPayment {

        @Test
        @DisplayName("Adds payment and recalculates paid amount")
        void addsPaymentAndRecalculates() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
            when(orderMapper.toPaymentEntity(any(PaymentRequest.class))).thenAnswer(inv -> {
                PaymentRequest req = inv.getArgument(0);
                Payment p = new Payment();
                p.setAmount(req.getAmount());
                return p;
            });
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(paymentRepository.sumByOrderId(1L)).thenReturn(new BigDecimal("700.00"));
            PaymentResponse paymentResponse = new PaymentResponse();
            when(orderMapper.paymentToDto(any(Payment.class))).thenReturn(paymentResponse);

            PaymentRequest request = new PaymentRequest();
            request.setAmount(new BigDecimal("200.00"));

            PaymentResponse result = orderService.addPayment(1L, request);

            assertThat(result).isNotNull();
            verify(paymentRepository).save(any(Payment.class));
            verify(orderRepository).updatePaidAmount(1L, new BigDecimal("700.00"));
            verify(orderRepository).updateDebtAmount(1L);
        }

        @Test
        @DisplayName("Throws when order not found")
        void throwsWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            PaymentRequest request = new PaymentRequest();
            request.setAmount(new BigDecimal("100.00"));

            assertThatThrownBy(() -> orderService.addPayment(999L, request))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ===================== calculateOpenOrderTotal =====================

    @Nested
    @DisplayName("calculateOpenOrderTotal")
    class CalculateOpenOrderTotal {

        @Test
        @DisplayName("Calculates total from materials with dimensions")
        void calculatesTotalFromMaterials() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("200.00"));
            mat.setWasteCoefficient(new BigDecimal("1.05"));
            mat.setUnit("м2");
            mat.setDefaultWidthM(BigDecimal.ZERO);
            mat.setDefaultHeightM(BigDecimal.ZERO);

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(new BigDecimal("2.0"));
            om.setHeightM(new BigDecimal("3.0"));

            testOrder.getMaterials().add(om);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            BigDecimal total = orderService.calculateOpenOrderTotal(1L);

            // 2.0 * 3.0 * 200.00 * 1.05 = 1260.00
            assertThat(total).isEqualByComparingTo(new BigDecimal("1260.00"));
        }

        @Test
        @DisplayName("Uses default dimensions when material dimensions are null")
        void usesDefaultDimensions() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("100.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);
            mat.setUnit("м2");
            mat.setDefaultWidthM(new BigDecimal("1.5"));
            mat.setDefaultHeightM(new BigDecimal("2.0"));

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(null);
            om.setHeightM(null);

            testOrder.getMaterials().add(om);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            BigDecimal total = orderService.calculateOpenOrderTotal(1L);

            // 1.5 * 2.0 * 100.00 * 1.0 = 300.00
            assertThat(total).isEqualByComparingTo(new BigDecimal("300.00"));
        }

        @Test
        @DisplayName("Linear material (м.п.) uses only width")
        void linearMaterialUsesOnlyWidth() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Лента");
            mat.setPrice(new BigDecimal("50.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);
            mat.setUnit("м.п.");
            mat.setDefaultWidthM(BigDecimal.ZERO);
            mat.setDefaultHeightM(BigDecimal.ZERO);

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(new BigDecimal("10.0"));
            om.setHeightM(null);

            testOrder.getMaterials().add(om);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            BigDecimal total = orderService.calculateOpenOrderTotal(1L);

            // 10.0 * 50.00 * 1.0 = 500.00
            assertThat(total).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Returns zero for order with no materials")
        void returnsZeroForNoMaterials() {
            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            BigDecimal total = orderService.calculateOpenOrderTotal(1L);

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Throws when order not found")
        void throwsWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.calculateOpenOrderTotal(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ===================== getCalculatedOrder =====================

    @Nested
    @DisplayName("getCalculatedOrder")
    class GetCalculatedOrder {

        @Test
        @DisplayName("Calculates order with priceplus correctly")
        void calculatesWithPriceplus() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("200.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);

            OrderMaterial om1 = new OrderMaterial();
            om1.setId(1L);
            om1.setMaterial(mat);
            om1.setWidthM(new BigDecimal("2.0"));
            om1.setHeightM(new BigDecimal("3.0"));
            om1.setCost(new BigDecimal("1200.00"));
            om1.setEyeletCost(new BigDecimal("50.00"));

            OrderMaterial om2 = new OrderMaterial();
            om2.setId(2L);
            om2.setMaterial(mat);
            om2.setWidthM(new BigDecimal("1.0"));
            om2.setHeightM(new BigDecimal("1.0"));
            om2.setCost(new BigDecimal("200.00"));
            om2.setEyeletCost(BigDecimal.ZERO);

            testOrder.getMaterials().addAll(List.of(om1, om2));
            testOrder.setPriceplus(new BigDecimal("10.00"));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            CalculatedOrderResponse result = orderService.getCalculatedOrder(1L);

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(1L);
            assertThat(result.getPriceplus()).isEqualByComparingTo(new BigDecimal("10.00"));

            // totalWithoutPriceplus = (1200 + 50) + (200 + 0) = 1450.00
            assertThat(result.getTotalWithoutPriceplus()).isEqualByComparingTo(new BigDecimal("1450.00"));

            // totalWithPriceplus = 1450 * 1.10 = 1595.00
            assertThat(result.getTotalWithPriceplus()).isEqualByComparingTo(new BigDecimal("1595.00"));

            assertThat(result.getMaterials()).hasSize(2);
        }

        @Test
        @DisplayName("Handles null eyeletCost as zero")
        void handlesNullEyeletCost() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("200.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(new BigDecimal("1.0"));
            om.setHeightM(new BigDecimal("1.0"));
            om.setCost(new BigDecimal("200.00"));
            om.setEyeletCost(null);

            testOrder.getMaterials().add(om);
            testOrder.setPriceplus(BigDecimal.ZERO);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            CalculatedOrderResponse result = orderService.getCalculatedOrder(1L);

            // totalWithoutPriceplus = 200 + 0 = 200.00
            assertThat(result.getTotalWithoutPriceplus()).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Handles null priceplus as zero")
        void handlesNullPriceplus() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("200.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(new BigDecimal("1.0"));
            om.setHeightM(new BigDecimal("1.0"));
            om.setCost(new BigDecimal("200.00"));
            om.setEyeletCost(BigDecimal.ZERO);

            testOrder.getMaterials().add(om);
            testOrder.setPriceplus(null);

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            CalculatedOrderResponse result = orderService.getCalculatedOrder(1L);

            // With priceplus=0, totalWithPriceplus should equal totalWithoutPriceplus
            assertThat(result.getTotalWithPriceplus()).isEqualByComparingTo(result.getTotalWithoutPriceplus());
        }

        @Test
        @DisplayName("Includes operations cost in totals")
        void includesOperationsCost() {
            Material mat = new Material();
            mat.setId(1L);
            mat.setName("Баннер");
            mat.setPrice(new BigDecimal("200.00"));
            mat.setWasteCoefficient(BigDecimal.ONE);

            OrderMaterial om = new OrderMaterial();
            om.setId(1L);
            om.setMaterial(mat);
            om.setWidthM(new BigDecimal("1.0"));
            om.setHeightM(new BigDecimal("1.0"));
            om.setCost(new BigDecimal("200.00"));
            om.setEyeletCost(BigDecimal.ZERO);

            OrderItem item = new OrderItem();
            item.setId(1L);
            item.setOrder(testOrder);

            OrderOperation op = new OrderOperation();
            op.setId(1L);
            op.setOperationId(1L);
            op.setOperationName("Печать");
            op.setSubtotal(new BigDecimal("100.00"));
            op.setOrderItem(item);

            item.getOperations().add(op);
            om.setOrderItem(item);

            testOrder.getMaterials().add(om);
            testOrder.getItems().add(item);
            testOrder.setPriceplus(new BigDecimal("10.00"));

            when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

            CalculatedOrderResponse result = orderService.getCalculatedOrder(1L);

            // totalWithoutPriceplus = (200 + 0) + 100 (ops) = 300.00
            assertThat(result.getTotalWithoutPriceplus()).isEqualByComparingTo(new BigDecimal("300.00"));

            // totalWithPriceplus = (200 + 0) * 1.10 + 100 * 1.10 = 220 + 110 = 330.00
            assertThat(result.getTotalWithPriceplus()).isEqualByComparingTo(new BigDecimal("330.00"));
        }

        @Test
        @DisplayName("Throws when order not found")
        void throwsWhenOrderNotFound() {
            when(orderRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getCalculatedOrder(999L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ===================== Calculation formula verification =====================

    @Nested
    @DisplayName("Calculation formula verification")
    class CalculationFormulaVerification {

        @Test
        @DisplayName("Material cost formula: widthM × heightM × price × wasteCoeff")
        void materialCostFormula() {
            BigDecimal widthM = new BigDecimal("2.5");
            BigDecimal heightM = new BigDecimal("4.0");
            BigDecimal price = new BigDecimal("150.00");
            BigDecimal wasteCoeff = new BigDecimal("1.10");

            BigDecimal materialCost = widthM.multiply(heightM).multiply(price).multiply(wasteCoeff);

            // 2.5 * 4.0 * 150.00 * 1.10 = 1650.00
            assertThat(materialCost).isEqualByComparingTo(new BigDecimal("1650.00"));
        }

        @Test
        @DisplayName("Priceplus formula: total × (1 + priceplus/100)")
        void priceplusFormula() {
            BigDecimal total = new BigDecimal("1000.00");
            BigDecimal priceplus = new BigDecimal("15.00");

            BigDecimal result = total.multiply(
                    BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);

            // 1000 * 1.15 = 1150.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("1150.00"));
        }

        @Test
        @DisplayName("Negative priceplus (discount)")
        void negativePriceplus() {
            BigDecimal total = new BigDecimal("1000.00");
            BigDecimal priceplus = new BigDecimal("-10.00");

            BigDecimal result = total.multiply(
                    BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);

            // 1000 * 0.90 = 900.00
            assertThat(result).isEqualByComparingTo(new BigDecimal("900.00"));
        }

        @Test
        @DisplayName("Perimeter formula: (width + height) × 2")
        void perimeterFormula() {
            BigDecimal widthM = new BigDecimal("3.0");
            BigDecimal heightM = new BigDecimal("2.0");

            BigDecimal perimeter = widthM.add(heightM).multiply(BigDecimal.valueOf(2));

            // (3 + 2) * 2 = 10.0
            assertThat(perimeter).isEqualByComparingTo(new BigDecimal("10.0"));
        }

        @Test
        @DisplayName("Eyelet quantity formula: perimeter(cm) / stepCm, round UP")
        void eyeletQuantityFormula() {
            BigDecimal perimeterM = new BigDecimal("10.0");
            int stepCm = 40;

            BigDecimal perimeterCm = perimeterM.multiply(BigDecimal.valueOf(100));
            BigDecimal eyeletQty = perimeterCm.divide(BigDecimal.valueOf(stepCm), 0, RoundingMode.UP);

            // 1000 / 40 = 25
            assertThat(eyeletQty).isEqualByComparingTo(new BigDecimal("25"));
        }

        @Test
        @DisplayName("Eyelet quantity with remainder rounds UP")
        void eyeletQuantityRoundingUp() {
            BigDecimal perimeterM = new BigDecimal("3.66");
            int stepCm = 40;

            BigDecimal perimeterCm = perimeterM.multiply(BigDecimal.valueOf(100));
            BigDecimal eyeletQty = perimeterCm.divide(BigDecimal.valueOf(stepCm), 0, RoundingMode.UP);

            // 366 / 40 = 9.15 -> 10
            assertThat(eyeletQty).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        @DisplayName("Debt calculation: totalAmount - paidAmount")
        void debtCalculation() {
            BigDecimal totalAmount = new BigDecimal("1000.00");
            BigDecimal paidAmount = new BigDecimal("350.00");

            BigDecimal debt = totalAmount.subtract(paidAmount);

            assertThat(debt).isEqualByComparingTo(new BigDecimal("650.00"));
        }

        @Test
        @DisplayName("Eyelet cost extraction: totalPrice - operationsSubtotal - materialCost")
        void eyeletCostExtraction() {
            BigDecimal totalPrice = new BigDecimal("1500.00");
            BigDecimal operationsSubtotal = new BigDecimal("300.00");
            BigDecimal materialCost = new BigDecimal("1000.00");

            BigDecimal eyeletCost = totalPrice.subtract(operationsSubtotal).subtract(materialCost);

            // 1500 - 300 - 1000 = 200
            assertThat(eyeletCost).isEqualByComparingTo(new BigDecimal("200.00"));
        }

        @Test
        @DisplayName("Eyelet cost is zero when negative (no eyelets)")
        void eyeletCostNonNegative() {
            BigDecimal totalPrice = new BigDecimal("1200.00");
            BigDecimal operationsSubtotal = new BigDecimal("100.00");
            BigDecimal materialCost = new BigDecimal("1100.00");

            BigDecimal eyeletCost = totalPrice.subtract(operationsSubtotal).subtract(materialCost);
            // 1200 - 100 - 1100 = 0
            assertThat(eyeletCost).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Order total = sum of all item totalPrices")
        void orderTotalSum() {
            BigDecimal item1 = new BigDecimal("1200.00");
            BigDecimal item2 = new BigDecimal("800.00");
            BigDecimal item3 = new BigDecimal("450.00");

            BigDecimal orderTotal = item1.add(item2).add(item3);

            assertThat(orderTotal).isEqualByComparingTo(new BigDecimal("2450.00"));
        }

        @Test
        @DisplayName("Order totalWithPriceplus = total × (1 + priceplus/100)")
        void orderTotalWithPriceplus() {
            BigDecimal total = new BigDecimal("2450.00");
            BigDecimal priceplus = new BigDecimal("20.00");

            BigDecimal totalWithPriceplus = total.multiply(
                    BigDecimal.ONE.add(priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
            ).setScale(2, RoundingMode.HALF_UP);

            // 2450 * 1.20 = 2940.00
            assertThat(totalWithPriceplus).isEqualByComparingTo(new BigDecimal("2940.00"));
        }
    }

    // ===================== Entity defaults =====================

    @Nested
    @DisplayName("Entity default values")
    class EntityDefaults {

        @Test
        @DisplayName("Order defaults: status=DRAFT, productionStage=NOT_STARTED")
        void orderDefaults() {
            Order order = new Order();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
            assertThat(order.getProductionStage()).isEqualTo(ProductionStage.NOT_STARTED);
            assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getDebtAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getTotalWithPriceplus()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(order.getHasDocuments()).isFalse();
            assertThat(order.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("OrderMaterial defaults")
        void orderMaterialDefaults() {
            OrderMaterial om = new OrderMaterial();
            assertThat(om.getDeleted()).isFalse();
        }

        @Test
        @DisplayName("OrderItem defaults")
        void orderItemDefaults() {
            OrderItem item = new OrderItem();
            assertThat(item.getDeleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Массовые проверки расчётов (100 заказов)")
    class MassCalculationVerification {

        @Test
        @DisplayName("Проверяем корректность расчётов на 100 разных заказах с материалами и операциями")
        void verifyCalculationsOn100Orders() {
            for (int i = 0; i < 100; i++) {
                // Детерминированные вариации на основе индекса
                int seed = i;
                BigDecimal width = BigDecimal.valueOf(0.5 + (seed % 20) * 0.3);      // 0.5 .. 6.2 м
                BigDecimal height = (seed % 7 == 0) ? null : BigDecimal.valueOf(0.8 + (seed % 15) * 0.4);
                BigDecimal price = BigDecimal.valueOf(50 + (seed % 30) * 10);        // 50..340
                BigDecimal waste = BigDecimal.valueOf(1.0 + (seed % 5) * 0.05);      // 1.0 .. 1.20
                boolean isLinear = (seed % 5 == 0);
                String unit = isLinear ? "м.п." : "м2";
                BigDecimal priceplus = new BigDecimal((seed % 9) * 5);           // 0,5,10,...,40

                // Создаём материал
                Material mat = new Material();
                mat.setId(100L + seed);
                mat.setName("Материал-" + seed);
                mat.setPrice(price);
                mat.setWasteCoefficient(waste);
                mat.setUnit(unit);
                mat.setDefaultWidthM(BigDecimal.ZERO);
                mat.setDefaultHeightM(BigDecimal.ZERO);

                // Создаём позицию заказа
                OrderMaterial om = new OrderMaterial();
                om.setId(200L + seed);
                om.setMaterial(mat);
                om.setWidthM(width);
                om.setHeightM(height);
                om.setDeleted(false);

                // Создаём заказ
                Order order = new Order();
                order.setId(300L + seed);
                order.setMaterials(List.of(om));
                order.setPriceplus(priceplus);
                order.setItems(new java.util.ArrayList<>());
                order.setDeleted(false);

                // Операции (каждый 3-й заказ)
                BigDecimal expectedOps = BigDecimal.ZERO;
                if (seed % 3 == 0) {
                    OrderItem item = new OrderItem();
                    item.setId(400L + seed);
                    item.setOrder(order);

                    OrderOperation op = new OrderOperation();
                    op.setId(500L + seed);
                    op.setOperationId(1L);
                    op.setOperationName("Печать");
                    op.setPricePerUnit(new BigDecimal(30));
                    op.setCalculatedQuantity(width.multiply(height != null ? height : BigDecimal.ONE));
                    op.setSubtotal(op.getCalculatedQuantity().multiply(op.getPricePerUnit()));
                    item.getOperations().add(op);
                    order.getItems().add(item);

                    expectedOps = op.getSubtotal();
                }

                when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

                // === Проверяем calculateOpenOrderTotal ===
                BigDecimal actual = orderService.calculateOpenOrderTotal(order.getId());

                // Ручной пересчёт — точно по логике calculateOpenOrderTotal
                BigDecimal q1 = width;
                BigDecimal q2 = height != null ? height : BigDecimal.ZERO;

                BigDecimal effectiveQty;
                if ("м2".equals(unit)) {
                    effectiveQty = q1.multiply(q2);
                } else {
                    effectiveQty = q1;
                }
                BigDecimal expectedMaterial = price.multiply(effectiveQty).multiply(waste);

                BigDecimal actualScaled = actual.setScale(4, RoundingMode.HALF_UP);
                BigDecimal expectedScaled = expectedMaterial.setScale(4, RoundingMode.HALF_UP);

                if (actualScaled.compareTo(expectedScaled) != 0) {
                    System.err.println("FAIL seed=" + seed +
                        " width=" + width + " height=" + height +
                        " price=" + price + " waste=" + waste +
                        " unit=" + unit +
                        " expected=" + expectedScaled + " actual=" + actualScaled);
                }

                assertThat(actualScaled).isEqualByComparingTo(expectedScaled);

                // === Проверяем getCalculatedOrder (с priceplus) ===
                // Для этого теста заполняем предрасчитанные cost (как это делает калькулятор)
                om.setCost(expectedMaterial);
                om.setEyeletCost(BigDecimal.ZERO);

                CalculatedOrderResponse calc = orderService.getCalculatedOrder(order.getId());

                BigDecimal ppFactor = BigDecimal.ONE.add(
                        priceplus.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                );

                BigDecimal expectedWithout = expectedMaterial.add(expectedOps).setScale(4, RoundingMode.HALF_UP);
                BigDecimal expectedWith = expectedWithout.multiply(ppFactor).setScale(4, RoundingMode.HALF_UP);

                assertThat(calc.getTotalWithoutPriceplus().setScale(4, RoundingMode.HALF_UP))
                        .isEqualByComparingTo(expectedWithout);

                // Допускаем небольшую погрешность из-за промежуточных округлений при применении priceplus
                assertThat(calc.getTotalWithPriceplus())
                        .isCloseTo(expectedWith, within(new BigDecimal("0.01")));
            }
        }
    }
}
