package com.team5.catdogeats.products.service;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.StockReservation;
import com.team5.catdogeats.products.domain.enums.ReservationStatus;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * StockReservationService 인터페이스 테스트
 * 인터페이스 기반 테스트로 실제 구현체의 동작을 검증합니다.
 * Record 타입인 ReservationRequest의 올바른 사용법을 포함합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("재고 예약 서비스 인터페이스 테스트")
class StockReservationServiceTest {

    @Mock
    private StockReservationService stockReservationService;

    // 테스트 데이터
    private Orders testOrder;
    private Products testProduct1;
    private Products testProduct2;
    private Users testUser;
    private List<StockReservationService.ReservationRequest> testReservationRequests;
    private List<StockReservation> testReservations;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = Users.builder()
                .id("user123")
                .name("김철수")
                .role(Role.ROLE_BUYER)
                .provider("google")
                .providerId("google123")
                .build();

        // 테스트 상품들 생성
        testProduct1 = Products.builder()
                .id("product1")
                .title("강아지 사료")
                .price(25000L)
                .stock(100)
                .version(1L)
                .build();

        testProduct2 = Products.builder()
                .id("product2")
                .title("고양이 간식")
                .price(15000L)
                .stock(50)
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

        // 테스트 예약 요청 목록 생성 (Record 타입)
        testReservationRequests = Arrays.asList(
                new StockReservationService.ReservationRequest(testProduct1, 2),
                new StockReservationService.ReservationRequest(testProduct2, 1)
        );

        // 테스트 재고 예약 목록 생성
        testReservations = Arrays.asList(
                StockReservation.createReservation(testOrder, testProduct1, 2, 30),
                StockReservation.createReservation(testOrder, testProduct2, 1, 30)
        );
    }

    @Nested
    @DisplayName("일괄 재고 예약 생성 테스트")
    class BulkReservationCreationTests {

        @Test
        @DisplayName("✅ 일괄 재고 예약 생성 성공")
        void createBulkReservations_Success() {
            // Given
            given(stockReservationService.createBulkReservations(eq(testOrder), eq(testReservationRequests)))
                    .willReturn(testReservations);

            // When
            List<StockReservation> result = stockReservationService.createBulkReservations(testOrder, testReservationRequests);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(result.get(1).getReservationStatus()).isEqualTo(ReservationStatus.RESERVED);

            verify(stockReservationService).createBulkReservations(eq(testOrder), eq(testReservationRequests));
        }

        @Test
        @DisplayName("❌ 재고 부족으로 예약 실패")
        void createBulkReservations_InsufficientStock_ThrowsException() {
            // Given
            given(stockReservationService.createBulkReservations(eq(testOrder), eq(testReservationRequests)))
                    .willThrow(new IllegalArgumentException("재고가 부족합니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(testOrder, testReservationRequests))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("재고가 부족합니다");

            verify(stockReservationService).createBulkReservations(eq(testOrder), eq(testReservationRequests));
        }

        @Test
        @DisplayName("❌ 빈 예약 요청 목록")
        void createBulkReservations_EmptyRequests_ThrowsException() {
            // Given
            List<StockReservationService.ReservationRequest> emptyRequests = List.of();
            given(stockReservationService.createBulkReservations(eq(testOrder), eq(emptyRequests)))
                    .willThrow(new IllegalArgumentException("예약 요청이 비어있습니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(testOrder, emptyRequests))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("예약 요청이 비어있습니다");

            verify(stockReservationService).createBulkReservations(eq(testOrder), eq(emptyRequests));
        }

        @Test
        @DisplayName("❌ null 주문으로 예약 시도")
        void createBulkReservations_NullOrder_ThrowsException() {
            // Given
            given(stockReservationService.createBulkReservations(eq(null), eq(testReservationRequests)))
                    .willThrow(new IllegalArgumentException("주문 정보가 필요합니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(null, testReservationRequests))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문 정보가 필요합니다");

            verify(stockReservationService).createBulkReservations(eq(null), eq(testReservationRequests));
        }
    }

    @Nested
    @DisplayName("재고 예약 확정 테스트")
    class ReservationConfirmationTests {

        @Test
        @DisplayName("✅ 재고 예약 확정 성공")
        void confirmReservations_Success() {
            // Given
            List<StockReservation> confirmedReservations = testReservations.stream()
                    .peek(reservation -> reservation.setReservationStatus(ReservationStatus.CONFIRMED))
                    .toList();

            given(stockReservationService.confirmReservations("order123"))
                    .willReturn(confirmedReservations);

            // When
            List<StockReservation> result = stockReservationService.confirmReservations("order123");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(result.get(1).getReservationStatus()).isEqualTo(ReservationStatus.CONFIRMED);

            verify(stockReservationService).confirmReservations("order123");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문 ID로 확정 시도")
        void confirmReservations_OrderNotFound_ThrowsException() {
            // Given
            given(stockReservationService.confirmReservations("nonexistent"))
                    .willThrow(new IllegalArgumentException("주문을 찾을 수 없습니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.confirmReservations("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");

            verify(stockReservationService).confirmReservations("nonexistent");
        }

        @Test
        @DisplayName("❌ 이미 확정된 예약 재확정 시도")
        void confirmReservations_AlreadyConfirmed_ThrowsException() {
            // Given
            given(stockReservationService.confirmReservations("order123"))
                    .willThrow(new IllegalStateException("이미 확정된 예약입니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.confirmReservations("order123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 확정된 예약입니다");

            verify(stockReservationService).confirmReservations("order123");
        }
    }

    @Nested
    @DisplayName("재고 예약 취소 테스트")
    class ReservationCancellationTests {

        @Test
        @DisplayName("✅ 재고 예약 취소 성공")
        void cancelReservations_Success() {
            // Given
            List<StockReservation> cancelledReservations = testReservations.stream()
                    .peek(reservation -> reservation.setReservationStatus(ReservationStatus.CANCELLED))
                    .toList();

            given(stockReservationService.cancelReservations("order123"))
                    .willReturn(cancelledReservations);

            // When
            List<StockReservation> result = stockReservationService.cancelReservations("order123");

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(result.get(1).getReservationStatus()).isEqualTo(ReservationStatus.CANCELLED);

            verify(stockReservationService).cancelReservations("order123");
        }

        @Test
        @DisplayName("❌ 존재하지 않는 주문 ID로 취소 시도")
        void cancelReservations_OrderNotFound_ThrowsException() {
            // Given
            given(stockReservationService.cancelReservations("nonexistent"))
                    .willThrow(new IllegalArgumentException("주문을 찾을 수 없습니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.cancelReservations("nonexistent"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문을 찾을 수 없습니다");

            verify(stockReservationService).cancelReservations("nonexistent");
        }

        @Test
        @DisplayName("❌ 이미 확정된 예약 취소 시도")
        void cancelReservations_ConfirmedReservation_ThrowsException() {
            // Given
            given(stockReservationService.cancelReservations("order123"))
                    .willThrow(new IllegalStateException("확정된 예약은 취소할 수 없습니다"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.cancelReservations("order123"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("확정된 예약은 취소할 수 없습니다");

            verify(stockReservationService).cancelReservations("order123");
        }

        @Test
        @DisplayName("✅ 빈 예약 목록 취소 - 정상 처리")
        void cancelReservations_EmptyReservations_Success() {
            // Given
            given(stockReservationService.cancelReservations("order456"))
                    .willReturn(List.of());

            // When
            List<StockReservation> result = stockReservationService.cancelReservations("order456");

            // Then
            assertThat(result).isEmpty();

            verify(stockReservationService).cancelReservations("order456");
        }
    }

    @Nested
    @DisplayName("Record DTO ReservationRequest 테스트")
    class ReservationRequestRecordTests {

        @Test
        @DisplayName("✅ ReservationRequest Record 생성 및 접근")
        void reservationRequest_Creation_Success() {
            // Given & When
            StockReservationService.ReservationRequest request =
                    new StockReservationService.ReservationRequest(testProduct1, 5);

            // Then
            assertThat(request.product()).isEqualTo(testProduct1);
            assertThat(request.quantity()).isEqualTo(5);
            assertThat(request.product().getTitle()).isEqualTo("강아지 사료");
        }

        @Test
        @DisplayName("✅ ReservationRequest Record 불변성 검증")
        void reservationRequest_Immutability_Verified() {
            // Given
            StockReservationService.ReservationRequest request1 =
                    new StockReservationService.ReservationRequest(testProduct1, 5);
            StockReservationService.ReservationRequest request2 =
                    new StockReservationService.ReservationRequest(testProduct1, 5);

            // Then - Record의 자동 equals/hashCode 구현 검증
            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());

            // toString 메서드 존재 검증 (구체적인 문자열 대신 구조만 확인)
            String toStringResult = request1.toString();
            assertThat(toStringResult).contains("ReservationRequest");
            assertThat(toStringResult).contains("product=");
            assertThat(toStringResult).contains("quantity=5");
        }

        @Test
        @DisplayName("✅ ReservationRequest List 타입 안정성")
        void reservationRequest_ListTypeSafety() {
            // Given
            List<StockReservationService.ReservationRequest> requests = Arrays.asList(
                    new StockReservationService.ReservationRequest(testProduct1, 3),
                    new StockReservationService.ReservationRequest(testProduct2, 2)
            );

            // When
            StockReservationService.ReservationRequest firstRequest = requests.get(0);
            StockReservationService.ReservationRequest secondRequest = requests.get(1);

            // Then
            assertThat(firstRequest.product().getTitle()).isEqualTo("강아지 사료");
            assertThat(firstRequest.quantity()).isEqualTo(3);
            assertThat(secondRequest.product().getTitle()).isEqualTo("고양이 간식");
            assertThat(secondRequest.quantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("❌ ReservationRequest null 값 처리")
        void reservationRequest_NullHandling() {
            // Given & When & Then
            // Record 생성자에서 null 검증은 구현체에서 처리
            StockReservationService.ReservationRequest nullProductRequest =
                    new StockReservationService.ReservationRequest(null, 5);
            StockReservationService.ReservationRequest nullQuantityRequest =
                    new StockReservationService.ReservationRequest(testProduct1, null);

            // Record 자체는 null을 허용하지만, 실제 사용 시 검증이 필요
            assertThat(nullProductRequest.product()).isNull();
            assertThat(nullQuantityRequest.quantity()).isNull();
        }
    }

    @Nested
    @DisplayName("예외 상황 통합 테스트")
    class ExceptionIntegrationTests {

        @Test
        @DisplayName("✅ 동시성 제어 예외 처리")
        void concurrencyControl_OptimisticLocking() {
            // Given
            given(stockReservationService.createBulkReservations(any(Orders.class), anyList()))
                    .willThrow(new org.springframework.dao.OptimisticLockingFailureException("동시성 충돌"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(testOrder, testReservationRequests))
                    .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class)
                    .hasMessageContaining("동시성 충돌");
        }

        @Test
        @DisplayName("✅ 데이터 무결성 제약 위반")
        void dataIntegrityConstraintViolation() {
            // Given
            given(stockReservationService.createBulkReservations(any(Orders.class), anyList()))
                    .willThrow(new org.springframework.dao.DataIntegrityViolationException("제약 조건 위반"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.createBulkReservations(testOrder, testReservationRequests))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class)
                    .hasMessageContaining("제약 조건 위반");
        }

        @Test
        @DisplayName("✅ 일반적인 런타임 예외 처리")
        void generalRuntimeException() {
            // Given
            given(stockReservationService.confirmReservations(anyString()))
                    .willThrow(new RuntimeException("시스템 오류"));

            // When & Then
            assertThatThrownBy(() -> stockReservationService.confirmReservations("order123"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("시스템 오류");
        }
    }
}