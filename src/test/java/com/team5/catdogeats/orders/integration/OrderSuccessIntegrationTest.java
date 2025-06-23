package com.team5.catdogeats.orders.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentMethod;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("✅ 주문‧결제 성공 시나리오")
class OrderSuccessIntegrationTest extends BaseOrderIntegrationTest {

    @Test
    @DisplayName("전체 플로우 성공")
    void completeFlow_success() throws Exception {
        given(tossPaymentsClient.confirmPayment(any())).willReturn(mockTossResponse);

        /* 1) 주문 생성 */
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();

        JsonNode dataNode = objectMapper.readTree(body).get("data");
        OrderCreateResponse res = objectMapper.treeToValue(dataNode, OrderCreateResponse.class);
        String orderId = res.getOrderId();

        /* 2) 재고 예약, 결제 정보 생성 대기 */
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                stockReservationRepository.findByOrderId(orderId).size() == 2);
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
                paymentRepository.findByOrdersId(orderId).isPresent());

        /* 3) 중간 상태 확인 */
        Orders saved = orderRepository.findById(orderId).orElseThrow();
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

        /* 4) 결제 성공 콜백 */
        mockTossResponse = mockTossResponse.toBuilder().orderId(orderId).build();
        given(tossPaymentsClient.confirmPayment(any())).willReturn(mockTossResponse);

        String paymentKey = paymentRepository.findByOrdersId(orderId).orElseThrow().getTossPaymentKey();
        mockMvc.perform(get("/v1/buyers/payments/success")
                        .param("paymentKey", paymentKey)
                        .param("orderId", orderId)
                        .param("amount", "65000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        /* 5) 최종 검증 */
        Orders finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_COMPLETED);

        Payments finalPayment = paymentRepository.findByOrdersId(orderId).orElseThrow();
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(finalPayment.getMethod()).isEqualTo(PaymentMethod.TOSS);

        List<StockReservation> finalReservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(finalReservations).allMatch(r -> r.getReservationStatus() == ReservationStatus.CONFIRMED);

        Products p1 = productRepository.findById(testProduct1.getId()).orElseThrow();
        Products p2 = productRepository.findById(testProduct2.getId()).orElseThrow();
        assertThat(p1.getStock()).isEqualTo(98);
        assertThat(p2.getStock()).isEqualTo(49);
    }
}
