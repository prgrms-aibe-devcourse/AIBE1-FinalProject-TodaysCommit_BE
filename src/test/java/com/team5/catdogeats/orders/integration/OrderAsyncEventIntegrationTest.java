package com.team5.catdogeats.orders.integration;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.products.domain.Products;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("✅ 비동기 이벤트 처리")
class OrderAsyncEventIntegrationTest extends BaseOrderIntegrationTest {

    @Test
    @DisplayName("이벤트 처리 순서 검증")
    void eventProcessingOrder() throws Exception {
        given(tossPaymentsClient.confirmPayment(any())).willReturn(mockTossResponse);

        var result = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testOrderRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        /* 재고 예약 → 결제 정보 순서 */
        Awaitility.await().atMost(8, TimeUnit.SECONDS).until(() ->
                stockReservationRepository.findByOrderId(orderId).size() == 2);
        Awaitility.await().atMost(8, TimeUnit.SECONDS).until(() ->
                paymentRepository.findByOrdersId(orderId).isPresent());

        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);

        Products p1 = productRepository.findById(testProduct1.getId()).orElseThrow();
        assertThat(p1.getStock()).isEqualTo(100); // 아직 차감 안됨
    }
}
