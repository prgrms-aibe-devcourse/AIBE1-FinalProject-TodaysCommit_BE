package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStatsDTO;
import com.team5.catdogeats.orders.mapper.SellerStoreStatsMapper;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler.OrderStatsRetrievalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerStoreStatsService 단위 테스트")
class SellerStoreStatsDTOServiceImplTest {

    @InjectMocks
    private SellerStoreStatsServiceImpl sellerStoreStatsService;

    @Mock
    private SellerStoreStatsMapper sellerStoreStatsMapper;

    private String testSellerId;
    private SellerStoreStatsDTO testStats;

    @BeforeEach
    void setUp() {
        testSellerId = "2aa4ad9f-dd05-4739-a683-eb8d2115635f";
        testStats = new SellerStoreStatsDTO(150L, 2.5, 200L);
    }

    @Nested
    @DisplayName("판매자 통계 조회 테스트")
    class SellerStatsQueryTests {

        @Test
        @DisplayName("성공 - 통계 데이터 조회")
        void getSellerStoreStats_Success() {
            // given
            given(sellerStoreStatsMapper.getSellerStoreStats(testSellerId))
                    .willReturn(testStats);

            // when
            SellerStoreStatsDTO result = sellerStoreStatsService.getSellerStoreStats(testSellerId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalSalesCount()).isEqualTo(150L);
            assertThat(result.avgDeliveryDays()).isEqualTo(2.5);
            assertThat(result.totalReviews()).isEqualTo(200L);

            verify(sellerStoreStatsMapper).getSellerStoreStats(testSellerId);
        }

        @Test
        @DisplayName("성공 - 통계 데이터 없음 (기본값 반환)")
        void getSellerStoreStats_NoData_ReturnsEmpty() {
            // given
            given(sellerStoreStatsMapper.getSellerStoreStats(testSellerId))
                    .willReturn(null);

            // when
            SellerStoreStatsDTO result = sellerStoreStatsService.getSellerStoreStats(testSellerId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalSalesCount()).isEqualTo(0L);
            assertThat(result.avgDeliveryDays()).isEqualTo(0.0);
            assertThat(result.totalReviews()).isEqualTo(0L);

            verify(sellerStoreStatsMapper).getSellerStoreStats(testSellerId);
        }

        @Test
        @DisplayName("실패 - 빈 판매자 ID")
        void getSellerStoreStats_EmptySellerId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreStatsService.getSellerStoreStats(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - null 판매자 ID")
        void getSellerStoreStats_NullSellerId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreStatsService.getSellerStoreStats(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 매퍼 오류")
        void getSellerStoreStats_MapperError_ThrowsException() {
            // given
            given(sellerStoreStatsMapper.getSellerStoreStats(testSellerId))
                    .willThrow(new RuntimeException("SQL 실행 오류"));

            // when & then
            assertThatThrownBy(() -> sellerStoreStatsService.getSellerStoreStats(testSellerId))
                    .isInstanceOf(OrderStatsRetrievalException.class)
                    .hasMessageContaining("판매자 집계 정보 조회 실패");
        }
    }

    @Nested
    @DisplayName("캐시 동작 테스트")
    class CacheTests {

        @Test
        @DisplayName("캐시 키 생성 확인")
        void getSellerStoreStats_CacheKey_Verification() {
            // given
            given(sellerStoreStatsMapper.getSellerStoreStats(testSellerId))
                    .willReturn(testStats);

            // when
            sellerStoreStatsService.getSellerStoreStats(testSellerId);

            // then
            // @Cacheable 어노테이션의 key = "#sellerId"
            verify(sellerStoreStatsMapper).getSellerStoreStats(testSellerId);
        }
    }
}