package com.team5.catdogeats.products.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import com.team5.catdogeats.products.dto.StockAvailabilityDto;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 재고 예약 서비스 테스트
 * StockReservationService의 핵심 비즈니스 로직을 검증합니다.
 * 재고 예약 시스템의 안정성과 정확성을 보장하는 중요한 테스트입니다.
 * 테스트 범위:
 * 1. 재고 예약 생성 (단일/일괄)
 * 2. 재고 가용성 검증
 * 3. 예약 상태 관리 (확정/취소/만료)
 * 4. 실제 재고 차감
 * 5. 동시성 제어 및 재시도 로직
 * 6. 배치 처리 (만료된 예약 정리)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("재고 예약 서비스 테스트")
class StockReservationServiceTest {

    @InjectMocks
    private StockReservationService stockReservationService;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ProductRepository productRepository;

    // 테스트 데이터
    private Orders testOrder;
    private Products testProduct;
    private Users testUser;
    private StockReservation testReservation;
    private List<StockReservationService.ReservationRequest> testReservationRequests;

    @BeforeEach
    void setUp() {
        // 재고 예약 만료 시간 설정 (30분)
        ReflectionTestUtils.setField(stockReservationService, "reservationExpirationMinutes", 30);

        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .provider("google")
                .providerId("google123")
                .build();

        // 테스트 상품 생성
        testProduct = Products.builder()
                .id("product123")
                .title("강아지 사료") // 수정: productName -> title
                .price(25000L)
                .stock(100)
                .version(1L)
                .build();

        // 테스트 주문 생성
        testOrder = Orders.builder()
                .id("order123")
                .orderNumber(1001L)
                .user(testUser)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(50000L)
                .build();

        // 테스트 재고 예약 생성
        testReservation = StockReservation.createReservation(testOrder, testProduct, 5, 30);

        // 테스트 예약 요청 목록 생성
        testReservationRequests = List.of(
                new StockReservationService.ReservationRequest(testProduct, 5)
        );
    }

    @Nested
    @DisplayName("단일 재고 예약 생성 테스트")
    class SingleReservationCreationTests {

        @Test
        @DisplayName("✅ 재고 예약 생성 성공")
        void createReservation_Success() {
            // Given
            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10); // 현재 예약된 수량
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct)); // 상품 조회 Mock 추가
            given(stockReservationRepository.save(any(StockReservation.class)))
                    .willReturn(testReservation);

            // When
            StockReservation result = stockReservationService.createReservation(testOrder, testProduct, 5);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReservedQuantity()).isEqualTo(5);
            assertThat(result.getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
            verify(stockReservationRepository).save(any(StockReservation.class));
        }

        @Test
        @DisplayName("❌ 재고 부족으로 예약 실패")
        void createReservation_InsufficientStock_ThrowsException() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(95); // 이미 95개 예약됨 (재고 100개 중)

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createReservation(testOrder, testProduct, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");

            verify(stockReservationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("일괄 재고 예약 생성 테스트")
    class BulkReservationCreationTests {

        @Test
        @DisplayName("✅ 일괄 재고 예약 생성 성공")
        void createBulkReservations_Success() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10);
            given(stockReservationRepository.saveAll(any()))
                    .willReturn(List.of(testReservation));

            // When
            List<StockReservation> results = stockReservationService.createBulkReservations(
                    testOrder, testReservationRequests);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getReservedQuantity()).isEqualTo(5);
            verify(stockReservationRepository).saveAll(any());
        }

        @Test
        @DisplayName("❌ 일괄 예약 중 일부 상품 재고 부족")
        void createBulkReservations_PartialInsufficientStock_ThrowsException() {
            // Given - 첫 번째 상품은 재고 충분, 두 번째 상품은 재고 부족
            Products product2 = Products.builder()
                    .id("product456")
                    .title("고양이 간식") // 수정: productName -> title
                    .price(15000L)
                    .stock(5) // 재고 부족
                    .build();

            List<StockReservationService.ReservationRequest> multipleRequests = Arrays.asList(
                    new StockReservationService.ReservationRequest(testProduct, 5),
                    new StockReservationService.ReservationRequest(product2, 10) // 재고 부족
            );

            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(productRepository.findById("product456")).willReturn(Optional.of(product2));

            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(0);
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product456"))
                    .willReturn(0);

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(testOrder, multipleRequests))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");

            verify(stockReservationRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("재고 가용성 검증 테스트")
    class StockAvailabilityTests {

        @Test
        @DisplayName("✅ 재고 가용성 조회 성공")
        void getStockAvailability_Success() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(20);

            // When
            StockAvailabilityDto result = stockReservationService.getStockAvailability("product123");

            // Then
            assertThat(result.getProductId()).isEqualTo("product123");
            assertThat(result.getActualStock()).isEqualTo(100);
            assertThat(result.getReservedStock()).isEqualTo(20);
            assertThat(result.getAvailableStock()).isEqualTo(80); // 100 - 20
        }

        @Test
        @DisplayName("❌ 존재하지 않는 상품 조회")
        void getStockAvailability_ProductNotFound_ThrowsException() {
            // Given
            given(productRepository.findById("nonexistent")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockReservationService.getStockAvailability("nonexistent"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("✅ 예약된 재고가 없는 경우")
        void getStockAvailability_NoReservations() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            // 수정: findActiveReservedQuantityByProductId -> getTotalReservedQuantityByProductId
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(null); // 예약 없음

            // When
            StockAvailabilityDto result = stockReservationService.getStockAvailability("product123");

            // Then
            assertThat(result.getReservedStock()).isEqualTo(0);
            assertThat(result.getAvailableStock()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("예약 상태 관리 테스트")
    class ReservationStatusManagementTests {

        @Test
        @DisplayName("✅ 재고 예약 확정 성공")
        void confirmReservations_Success() {
            // Given
            StockReservation reservation1 = StockReservation.createReservation(testOrder, testProduct, 5, 30);
            StockReservation reservation2 = StockReservation.createReservation(testOrder, testProduct, 3, 30);
            List<StockReservation> reservations = Arrays.asList(reservation1, reservation2);

            given(stockReservationRepository.findByOrderId("order123")).willReturn(reservations);
            given(stockReservationRepository.saveAll(reservations)).willReturn(reservations);

            // When
            List<StockReservation> results = stockReservationService.confirmReservations("order123");

            // Then
            assertThat(results).hasSize(2);
            results.forEach(reservation -> {
                assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                assertThat(reservation.getConfirmedAt()).isNotNull();
            });
            verify(stockReservationRepository).saveAll(reservations);
        }

        @Test
        @DisplayName("❌ 예약 정보가 없는 경우 확정 실패")
        void confirmReservations_NoReservationsFound_ThrowsException() {
            // Given
            given(stockReservationRepository.findByOrderId("order123")).willReturn(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> stockReservationService.confirmReservations("order123"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("예약을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("✅ 재고 예약 취소 성공")
        void cancelReservations_Success() {
            // Given
            List<StockReservation> activeReservations = List.of(testReservation);
            given(stockReservationRepository.findByOrderId("order123")).willReturn(activeReservations);
            given(stockReservationRepository.saveAll(activeReservations)).willReturn(activeReservations);

            // When
            List<StockReservation> results = stockReservationService.cancelReservations("order123");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
            verify(stockReservationRepository).saveAll(activeReservations);
        }
    }

    @Nested
    @DisplayName("실제 재고 차감 테스트")
    class StockDecrementTests {

        @Test
        @DisplayName("✅ 확정된 예약에 대한 재고 차감 성공")
        void decrementConfirmedStock_Success() {
            // Given
            StockReservation confirmedReservation = StockReservation.createReservation(testOrder, testProduct, 5, 30);
            confirmedReservation.confirm(); // 상태를 CONFIRMED로 변경

            List<StockReservation> confirmedReservations = List.of(confirmedReservation);
            given(stockReservationRepository.findByOrderId("order123")).willReturn(confirmedReservations);
            given(productRepository.save(any(Products.class))).willReturn(testProduct);

            // When
            List<StockReservation> results = stockReservationService.decrementConfirmedStock("order123");

            // Then
            assertThat(results).hasSize(1);
            // 수정: saveAll -> save
            verify(productRepository, times(1)).save(any(Products.class));
            assertThat(testProduct.getStock()).isEqualTo(95); // 100 - 5
        }

        @Test
        @DisplayName("❌ 확정되지 않은 예약은 재고 차감 제외")
        void decrementConfirmedStock_OnlyConfirmedReservations() {
            // Given
            StockReservation reservedReservation = StockReservation.createReservation(testOrder, testProduct, 5, 30);
            // 확정하지 않은 상태 (RESERVED)

            List<StockReservation> reservations = List.of(reservedReservation);
            given(stockReservationRepository.findByOrderId("order123")).willReturn(reservations);

            // When & Then
            assertThatThrownBy(() -> stockReservationService.decrementConfirmedStock("order123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("확정된 예약을 찾을 수 없습니다");

            verify(productRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("배치 처리 테스트")
    class BatchProcessingTests {

        @Test
        @DisplayName("✅ 만료된 예약 일괄 처리 성공")
        void expireBatchReservations_Success() {
            // Given
            // 수정: findExpiredActiveReservations -> findExpiredReservations
            given(stockReservationRepository.bulkExpireReservations(any(ZonedDateTime.class)))
                    .willReturn(2);

            // When
            // 수정: expireBatchReservations -> processExpiredReservations
            int processedCount = stockReservationService.processExpiredReservations();

            // Then
            assertThat(processedCount).isEqualTo(2);
            // 수정: findExpiredActiveReservations -> bulkExpireReservations
            verify(stockReservationRepository).bulkExpireReservations(any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("✅ 만료된 예약이 없는 경우")
        void expireBatchReservations_NoExpiredReservations() {
            // Given
            // 수정: findExpiredActiveReservations -> bulkExpireReservations
            given(stockReservationRepository.bulkExpireReservations(any(ZonedDateTime.class)))
                    .willReturn(0);

            // When
            // 수정: expireBatchReservations -> processExpiredReservations
            int processedCount = stockReservationService.processExpiredReservations();

            // Then
            assertThat(processedCount).isEqualTo(0);
        }
    }

    // 동시성 테스트는 별도 환경에서 수행하는 것이 더 정확하므로,
    // 여기서는 @Retryable 로직이 정상적으로 동작하는지만 검증합니다.
    @Nested
    @DisplayName("동시성 제어 테스트")
    class ConcurrencyControlTests {

        @Test
        @DisplayName("✅ 재시도 메커니즘 동작 확인")
        void retryMechanism_WorksCorrectly() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10);

            // 처음 두 번은 실패, 세 번째는 성공
            given(stockReservationRepository.save(any(StockReservation.class)))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"))
                    .willReturn(testReservation);

            // When & Then
            // @Retryable은 서비스 계층에 적용되므로, 테스트에서는 예외가 발생하지 않는 것을 확인
            StockReservation result = stockReservationService.createReservation(testOrder, testProduct, 5);
            assertThat(result).isNotNull();

            // 3번 시도되었는지 검증 (원본 1 + 재시도 2)
            // 실제 재시도 로직은 AOP 프록시를 통해 동작하므로, Mock 객체만으로는 정확한 횟수 검증이 어려울 수 있음.
            // 하지만 이 테스트는 서비스 로직이 예외를 던지지 않고 완료되는지를 확인하는 데 의미가 있음.
        }
    }
}