package com.team5.catdogeats.orders.integration;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.products.domain.Products;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("✅ 동시성 시나리오")
class OrderConcurrencyIntegrationTest extends BaseOrderIntegrationTest {

    @Test
    @DisplayName("재고 예약 경합 처리")
    void concurrentOrders_stockRace() throws Exception {
        /* 재고 5짜리 상품 */
        Products limited = productRepository.save(Products.builder()
                .seller(testProduct1.getSeller())
                .title("한정판").contents("재고 제한")
                .productNumber(ThreadLocalRandom.current().nextLong(100000, 999999))
                .price(50_000L).stock(5).build());

        OrderCreateRequest req = testOrderRequest.toBuilder()
                .orderItems(List.of(
                        OrderCreateRequest.OrderItemRequest.builder()
                                .productId(limited.getId()).quantity(3).build()))
                .build();

        var r1 = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        var r2 = mockMvc.perform(post("/v1/buyers/orders")
                        .with(authentication(testAuthentication)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String id1 = objectMapper.readTree(r1.getResponse().getContentAsString())
                .get("data").get("orderId").asText();
        String id2 = objectMapper.readTree(r2.getResponse().getContentAsString())
                .get("data").get("orderId").asText();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            Orders o1 = orderRepository.findById(id1).orElse(null);
            Orders o2 = orderRepository.findById(id2).orElse(null);
            return o1 != null && o2 != null &&
                    (o1.getOrderStatus() != o2.getOrderStatus());  // 하나는 취소, 하나는 결제대기
        });

        Products finalProduct = productRepository.findById(limited.getId()).orElseThrow();
        assertThat(finalProduct.getStock()).isEqualTo(2); // 5 - 3
    }
}
