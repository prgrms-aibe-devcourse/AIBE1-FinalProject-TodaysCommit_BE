package com.team5.catdogeats.orders.integration;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("❌ 주문‧결제 실패 시나리오")
class OrderFailureIntegrationTest extends BaseOrderIntegrationTest {

    @Test
    @DisplayName("재고 부족시 자동 취소")
    void insufficientStock_cancelsOrder() throws Exception {
        var excessive = testOrderRequest.toBuilder()
                .orderItems(List.of(testOrderRequest.getOrderItems().get(0).toBuilder().quantity(150).build()))
                .build();

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(excessive)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("결제 실패 시 재고 예약 취소")
    void paymentFail_cancelsReservation() throws Exception {
        /* 주문 생성 */
        MvcResult result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        Awaitility.await().atMost(8, TimeUnit.SECONDS).until(() ->
                stockReservationRepository.findByOrderId(orderId).size() == 2);

        /* 결제 실패 콜백 */
        mockMvc.perform(get("/v1/buyers/payments/fail")
                        .param("code", "INSUFFICIENT_BALANCE")
                        .param("message", "잔액 부족")
                        .param("orderId", orderId))
                .andExpect(status().isOk());

        /* 검증 */
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(reservations).allMatch(r -> r.getReservationStatus() == ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID 주문")
    void nonexistentProduct_rejected() throws Exception {
        var invalid = testOrderRequest.toBuilder()
                .orderItems(List.of(
                        testOrderRequest.getOrderItems().get(0).toBuilder()
                                .productId(UUID.randomUUID().toString()).build()))
                .build();

        mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isNotFound());
    }
}
