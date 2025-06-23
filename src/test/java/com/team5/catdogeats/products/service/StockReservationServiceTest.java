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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
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
                .title("강아지 사료")
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
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10); // 현재 예약된 수량
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
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
        @DisplayName("❌ 일괄 예약 중 재고 부족 실패")
        void createBulkReservations_PartialInsufficientStock_ThrowsException() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(98); // 재고 부족 상황

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(
                    testOrder, testReservationRequests))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");

            verify(stockReservationRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("재고 가용성 검증 테스트")
    class StockAvailabilityTests {

        @Test
        @DisplayName("✅ 재고 가용성 정보 조회 성공")
        void getStockAvailability_Success() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(20);

            // When
            StockAvailabilityDto result = stockReservationService.getStockAvailability("product123");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getActualStock()).isEqualTo(100);
            assertThat(result.getReservedStock()).isEqualTo(20);
            assertThat(result.getAvailableStock()).isEqualTo(80);
            assertThat(result.canReserve(10)).isTrue();
            assertThat(result.canReserve(90)).isFalse();
        }

        @Test
        @DisplayName("❌ 재고 부족 상황에서 가용성 확인")
        void getStockAvailability_InsufficientStock() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(95);

            // When
            StockAvailabilityDto result = stockReservationService.getStockAvailability("product123");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getActualStock()).isEqualTo(100);
            assertThat(result.getReservedStock()).isEqualTo(95);
            assertThat(result.getAvailableStock()).isEqualTo(5);
            assertThat(result.canReserve(10)).isFalse();
            assertThat(result.canReserve(5)).isTrue();
        }

        @Test
        @DisplayName("❌ 상품을 찾을 수 없는 경우")
        void getStockAvailability_ProductNotFound_ThrowsException() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> stockReservationService.getStockAvailability("product123"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("✅ 재고 가용성 검증 메서드 테스트")
        void validateStockAvailability_Success() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10);

            // When & Then
            // 가용 재고 내에서 요청하면 예외가 발생하지 않음
            assertThatCode(() -> stockReservationService.validateStockAvailability("product123", 50))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("❌ 재고 가용성 검증 실패")
        void validateStockAvailability_InsufficientStock() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(95);

            // When & Then
            assertThatThrownBy(() -> stockReservationService.validateStockAvailability("product123", 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }
    }

    @Nested
    @DisplayName("예약 상태 관리 테스트")
    class ReservationStatusManagementTests {

        @Test
        @DisplayName("✅ 재고 예약 확정 성공")
        void confirmReservations_Success() {
            // Given
            List<StockReservation> reservations = List.of(testReservation);
            given(stockReservationRepository.findByOrderId("order123")).willReturn(reservations);
            given(stockReservationRepository.saveAll(reservations)).willReturn(reservations);

            // When
            List<StockReservation> results = stockReservationService.confirmReservations("order123");

            // Then
            assertThat(results).hasSize(1);
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

            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("조회 메서드 테스트")
    class RetrievalMethodsTests {

        @Test
        @DisplayName("✅ 주문의 활성 예약 목록 조회")
        void getActiveReservationsByOrder_Success() {
            // Given
            StockReservation activeReservation = testReservation; // RESERVED 상태
            StockReservation cancelledReservation = StockReservation.createReservation(testOrder, testProduct, 3, 30);
            cancelledReservation.cancel(); // CANCELLED 상태

            List<StockReservation> allReservations = List.of(activeReservation, cancelledReservation);
            given(stockReservationRepository.findByOrderId("order123")).willReturn(allReservations);

            // When
            List<StockReservation> activeReservations = stockReservationService.getActiveReservationsByOrder("order123");

            // Then
            assertThat(activeReservations).hasSize(1);
            assertThat(activeReservations.get(0).getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
        }

        @Test
        @DisplayName("✅ 상품의 활성 예약 목록 조회")
        void getActiveReservationsByProduct_Success() {
            // Given
            List<StockReservation> activeReservations = List.of(testReservation);
            given(stockReservationRepository.findActiveReservationsByProductId("product123"))
                    .willReturn(activeReservations);

            // When
            List<StockReservation> results = stockReservationService.getActiveReservationsByProduct("product123");

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
            verify(stockReservationRepository).findActiveReservationsByProductId("product123");
        }
    }

    @Nested
    @DisplayName("배치 처리 테스트")
    class BatchProcessingTests {

        @Test
        @DisplayName("✅ 만료된 예약 일괄 처리 성공")
        void processExpiredReservations_Success() {
            // Given
            given(stockReservationRepository.bulkExpireReservations(any(ZonedDateTime.class)))
                    .willReturn(2);

            // When
            int processedCount = stockReservationService.processExpiredReservations();

            // Then
            assertThat(processedCount).isEqualTo(2);
            verify(stockReservationRepository).bulkExpireReservations(any(ZonedDateTime.class));
        }

        @Test
        @DisplayName("✅ 만료된 예약이 없는 경우")
        void processExpiredReservations_NoExpiredReservations() {
            // Given
            given(stockReservationRepository.bulkExpireReservations(any(ZonedDateTime.class)))
                    .willReturn(0);

            // When
            int processedCount = stockReservationService.processExpiredReservations();

            // Then
            assertThat(processedCount).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("동시성 제어 테스트")
    class ConcurrencyControlTests {

        @Test
        @DisplayName("✅ OptimisticLockingFailureException 핸들링 검증")
        void optimisticLockException_HandlingVerification() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10);

            // Mock으로 동시성 충돌 시뮬레이션
            given(stockReservationRepository.save(any(StockReservation.class)))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"));

            // When & Then
            // 동시성 충돌 시 예외가 던져지는 것을 확인
            assertThatThrownBy(() ->
                    stockReservationService.createReservation(testOrder, testProduct, 5))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessageContaining("동시성 충돌");

            // save 메서드가 호출되었는지 검증
            verify(stockReservationRepository).save(any(StockReservation.class));
        }

        @Test
        @DisplayName("✅ 재시도 성공 시나리오 Mock 검증")
        void retrySuccessScenario_MockVerification() {
            // Given
            given(productRepository.findById("product123")).willReturn(Optional.of(testProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("product123"))
                    .willReturn(10);

            // 첫 번째 시도는 실패, 두 번째 시도는 성공
            given(stockReservationRepository.save(any(StockReservation.class)))
                    .willThrow(new OptimisticLockingFailureException("동시성 충돌"))
                    .willReturn(testReservation);

            // When
            // @Retryable이 적용된 메서드를 직접 호출하는 대신
            // 재시도 로직을 포함한 별도 메서드로 테스트
            StockReservation result = attemptReservationWithRetry();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getReservedQuantity()).isEqualTo(5);

            // 재시도로 인해 save가 2번 호출되었는지 검증
            verify(stockReservationRepository, times(2)).save(any(StockReservation.class));
        }

        /**
         * 재시도 로직을 시뮬레이션하는 헬퍼 메서드
         * 실제 @Retryable 어노테이션 대신 수동으로 재시도를 구현
         */
        private StockReservation attemptReservationWithRetry() {
            int maxAttempts = 3;
            int currentAttempt = 0;

            while (true) {
                try {
                    currentAttempt++;
                    return stockReservationService.createReservation(testOrder, testProduct, 5);
                } catch (OptimisticLockingFailureException e) {
                    if (currentAttempt >= maxAttempts) {
                        throw e;
                    }
                    // 재시도 전 짧은 대기 (실제 환경에서는 @Retryable이 처리)
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                }
            }
        }

        @Test
        @DisplayName("✅ 동시성 제어 비즈니스 로직 검증")
        void concurrencyControlLogic_BusinessRuleVerification() {
            // Given - 동시에 여러 주문이 같은 상품을 예약하려는 상황 시뮬레이션
            Products limitedProduct = Products.builder()
                    .id("limited-product")
                    .title("한정 상품")
                    .price(10000L)
                    .stock(5) // 재고 5개만 있음
                    .version(1L)
                    .build();

            given(productRepository.findById("limited-product")).willReturn(Optional.of(limitedProduct));

            // 이미 4개가 예약된 상황
            given(stockReservationRepository.getTotalReservedQuantityByProductId("limited-product"))
                    .willReturn(4);

            // When & Then - 2개를 예약하려고 하면 재고 부족으로 실패해야 함
            assertThatThrownBy(() ->
                    stockReservationService.createReservation(testOrder, limitedProduct, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");

            // 실제 저장은 시도되지 않아야 함
            verify(stockReservationRepository, never()).save(any(StockReservation.class));
        }

        @Test
        @DisplayName("✅ 버전 기반 낙관적 락 검증")
        void optimisticLockVersionCheck() {
            // Given
            Products versionedProduct = Products.builder()
                    .id("versioned-product")
                    .title("버전 관리 상품")
                    .price(10000L)
                    .stock(10)
                    .version(1L) // 초기 버전
                    .build();

            given(productRepository.findById("versioned-product")).willReturn(Optional.of(versionedProduct));
            given(stockReservationRepository.getTotalReservedQuantityByProductId("versioned-product"))
                    .willReturn(0);

            // 버전 충돌 시뮬레이션 (다른 트랜잭션에서 상품을 수정함)
            given(stockReservationRepository.save(any(StockReservation.class)))
                    .willThrow(new OptimisticLockingFailureException("버전이 변경되었습니다"));

            // When & Then
            assertThatThrownBy(() ->
                    stockReservationService.createReservation(testOrder, versionedProduct, 1))
                    .isInstanceOf(OptimisticLockingFailureException.class)
                    .hasMessageContaining("버전이 변경되었습니다");
        }

        // ⚠️ 실제 멀티스레드 동시성 테스트는 비활성화
        // 이유: 테스트 환경에서는 예측하기 어렵고 불안정함
        @Test
        @Disabled("실제 동시성 테스트는 별도 환경에서 수행")
        @DisplayName("❌ 실제 멀티스레드 동시성 테스트 (비활성화)")
        void actualConcurrencyTest_Disabled() {
            // 이 테스트는 실제 프로덕션 환경이나 별도의 성능 테스트 환경에서만 수행
            // 단위 테스트에서는 Mock을 사용한 시나리오 검증으로 충분
        }
    }
}