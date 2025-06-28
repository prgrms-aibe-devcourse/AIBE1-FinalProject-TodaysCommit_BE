package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import com.team5.catdogeats.products.service.ProductStockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockManagerImpl implements ProductStockManager {

    private final ProductRepository productRepository;
    private final StockReservationRepository stockReservationRepository;

    @Override
    @JpaTransactional
    public void decrementStockForConfirmedReservations(String orderId) {
        log.info("확정된 재고 차감 시작: orderId={}", orderId);

        List<StockReservation> confirmedReservations = stockReservationRepository.findByOrderId(orderId)
                .stream()
                .filter(reservation -> reservation.getReservationStatus() == ReservationStatus.CONFIRMED)
                .toList();

        if (confirmedReservations.isEmpty()) {
            throw new IllegalStateException("재고를 차감할 확정된 예약을 찾을 수 없습니다: orderId=" + orderId);
        }

        for (StockReservation reservation : confirmedReservations) {
            Products product = reservation.getProduct();
            Integer decrementQuantity = reservation.getReservedQuantity();

            if (product.getStock() < decrementQuantity) {
                log.error("재고 부족으로 차감 실패: productId={}, 현재재고={}, 차감요청={}",
                        product.getId(), product.getStock(), decrementQuantity);
                throw new IllegalStateException(
                        String.format("재고 부족: 상품ID=%s, 현재재고=%d, 차감요청=%d",
                                product.getId(), product.getStock(), decrementQuantity));
            }

            product.decreaseStock(decrementQuantity);
            productRepository.save(product);

            log.info("재고 차감 완료: productId={}, 차감수량={}, 남은재고={}",
                    product.getId(), decrementQuantity, product.getStock());
        }

        log.info("확정된 재고 차감 완료: orderId={}, 처리된 예약 개수={}",
                orderId, confirmedReservations.size());
    }
}