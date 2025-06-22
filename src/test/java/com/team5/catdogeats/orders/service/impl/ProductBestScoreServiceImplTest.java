package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.mapper.ProductBestScoreMapper;
import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreData;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler.OrderStatsRetrievalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductBestScoreService 단위 테스트")
class ProductBestScoreServiceImplTest {

    @InjectMocks
    private ProductBestScoreServiceImpl productBestScoreService;

    @Mock
    private ProductBestScoreMapper productBestScoreMapper;

    private String testSellerId;
    private List<ProductBestScoreData> testScoreData;

    @BeforeEach
    void setUp() {
        testSellerId = "2aa4ad9f-dd05-4739-a683-eb8d2115635f";
        testScoreData = Arrays.asList(
                new ProductBestScoreData("product1", 100L, 1000000L, 4.5, 100L, 30L),
                new ProductBestScoreData("product2", 80L, 800000L, 4.0, 50L, 20L),
                new ProductBestScoreData("product3", 120L, 1200000L, 4.8, 120L, 40L)
        );
    }

    @Nested
    @DisplayName("베스트 점수 데이터 조회 테스트")
    class BestScoreDataQueryTests {

        @Test
        @DisplayName("성공 - 베스트 점수 데이터 조회")
        void getProductBestScoreData_Success() {
            // given
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(testScoreData);

            // when
            List<ProductBestScoreData> result = productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(3);
            assertThat(result.get(0).productId()).isEqualTo("product1");
            assertThat(result.get(0).salesQuantity()).isEqualTo(100L);
            assertThat(result.get(0).totalRevenue()).isEqualTo(1000000L);
            assertThat(result.get(0).avgRating()).isEqualTo(4.5);
            assertThat(result.get(0).reviewCount()).isEqualTo(100L);
            assertThat(result.get(0).recentOrderCount()).isEqualTo(30L);

            verify(productBestScoreMapper).getProductBestScoreDataBySeller(testSellerId);
        }

        @Test
        @DisplayName("성공 - 베스트 점수 데이터 없음")
        void getProductBestScoreData_NoData_ReturnsEmpty() {
            // given
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(Collections.emptyList());

            // when
            List<ProductBestScoreData> result = productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isEmpty();

            verify(productBestScoreMapper).getProductBestScoreDataBySeller(testSellerId);
        }

        @Test
        @DisplayName("성공 - 단일 상품 데이터")
        void getProductBestScoreData_SingleProduct_Success() {
            // given
            List<ProductBestScoreData> singleProductData = List.of(
                    new ProductBestScoreData("product1", 50L, 500000L, 4.2, 75L, 15L)
            );
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(singleProductData);

            // when
            List<ProductBestScoreData> result = productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo("product1");
            assertThat(result.get(0).salesQuantity()).isEqualTo(50L);
        }

        @Test
        @DisplayName("실패 - null 판매자 ID")
        void getProductBestScoreData_NullSellerId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> productBestScoreService.getProductBestScoreData(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 빈 판매자 ID")
        void getProductBestScoreData_EmptySellerId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> productBestScoreService.getProductBestScoreData(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 공백 판매자 ID")
        void getProductBestScoreData_WhitespaceSellerId_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> productBestScoreService.getProductBestScoreData("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 매퍼 SQL 실행 오류")
        void getProductBestScoreData_MapperSqlError_ThrowsException() {
            // given
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willThrow(new RuntimeException("SQL 실행 오류"));

            // when & then
            assertThatThrownBy(() -> productBestScoreService.getProductBestScoreData(testSellerId))
                    .isInstanceOf(OrderStatsRetrievalException.class)
                    .hasMessageContaining("베스트 점수 데이터 조회 실패")
                    .hasMessageContaining(testSellerId);
        }

        @Test
        @DisplayName("실패 - 매퍼 데이터베이스 연결 오류")
        void getProductBestScoreData_MapperDatabaseError_ThrowsException() {
            // given
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willThrow(new RuntimeException("데이터베이스 연결 실패"));

            // when & then
            assertThatThrownBy(() -> productBestScoreService.getProductBestScoreData(testSellerId))
                    .isInstanceOf(OrderStatsRetrievalException.class)
                    .hasMessageContaining("베스트 점수 데이터 조회 실패");
        }
    }

    @Nested
    @DisplayName("베스트 점수 계산 로직 테스트")
    class BestScoreCalculationTests {

        @Test
        @DisplayName("베스트 점수 계산 검증 - 표준 케이스")
        void calculateBestScore_StandardCase_Verification() {
            // given
            ProductBestScoreData scoreData = new ProductBestScoreData(
                    "product1",
                    100L,    // 판매량 (기준: 100 = 100점)
                    1000000L, // 매출액 (기준: 100만원 = 100점)
                    4.5,     // 평균 평점 (5점 만점에서 4.5 = 90점)
                    50L,     // 리뷰 수 (기준: 50 = 100점)
                    20L      // 최근 주문 수 (기준: 20 = 100점)
            );

            // when
            Double bestScore = scoreData.calculateBestScore();

            // then
            assertThat(bestScore).isNotNull();
            assertThat(bestScore).isGreaterThan(0);
            assertThat(bestScore).isLessThanOrEqualTo(100.0);

            // 실제 계산값 확인: 98.5
            // (100*0.4) + (100*0.3) + (90*0.15) + (100*0.1) + (100*0.05) = 40 + 30 + 13.5 + 10 + 5 = 98.5
            assertThat(bestScore).isEqualTo(98.5);
        }

        @Test
        @DisplayName("베스트 점수 계산 - 최고 성과 상품")
        void calculateBestScore_HighPerformanceProduct() {
            // given - 모든 지표가 최대 기준치
            ProductBestScoreData highPerformanceData = new ProductBestScoreData(
                    "product1",
                    100L,    // 최대 기준치
                    1000000L, // 100만원 (최대 기준치)
                    5.0,     // 만점
                    50L,     // 최대 기준치
                    20L      // 최대 기준치
            );

            // when
            Double bestScore = highPerformanceData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(100.0); // 모든 지표가 만점이므로 100점
        }

        @Test
        @DisplayName("베스트 점수 계산 - 최저 성과 상품")
        void calculateBestScore_LowPerformanceProduct() {
            // given - 모든 지표가 0
            ProductBestScoreData lowPerformanceData = new ProductBestScoreData(
                    "product1",
                    0L,      // 판매량 0
                    0L,      // 매출액 0
                    0.0,     // 평점 0
                    0L,      // 리뷰 0
                    0L       // 최근 주문 0
            );

            // when
            Double bestScore = lowPerformanceData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(0.0); // 모든 지표가 0이므로 0점
        }

        @Test
        @DisplayName("베스트 점수 계산 - 부분적 높은 성과")
        void calculateBestScore_PartialHighPerformance() {
            // given - 판매량은 높지만 다른 지표는 낮음
            ProductBestScoreData partialData = new ProductBestScoreData(
                    "product1",
                    200L,    // 기준치의 2배 (100점으로 제한됨)
                    500000L, // 기준치의 절반 (50점)
                    2.5,     // 5점 만점에서 2.5 (50점)
                    25L,     // 기준치의 절반 (50점)
                    10L      // 기준치의 절반 (50점)
            );

            // when
            Double bestScore = partialData.calculateBestScore();

            // then
            // 실제 계산: (100*0.4) + (50*0.3) + (50*0.15) + (50*0.1) + (50*0.05) = 40 + 15 + 7.5 + 5 + 2.5 = 70.0
            assertThat(bestScore).isEqualTo(70.0);
        }

        @Test
        @DisplayName("베스트 점수 계산 - null 값 처리")
        void calculateBestScore_NullValues() {
            // given - null 값들이 포함된 데이터
            ProductBestScoreData nullData = new ProductBestScoreData(
                    "product1",
                    null,    // null 판매량
                    null,    // null 매출액
                    null,    // null 평점
                    null,    // null 리뷰수
                    null     // null 최근주문수
            );

            // when
            Double bestScore = nullData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(0.0); // null은 0으로 처리되어야 함
        }

        @Test
        @DisplayName("베스트 점수 계산 - 소수점 처리")
        void calculateBestScore_DecimalHandling() {
            // given
            ProductBestScoreData decimalData = new ProductBestScoreData(
                    "product1",
                    75L,     // 75점
                    750000L, // 75점
                    3.75,    // 75점
                    37L,     // 74점 (37/50*100 = 74)
                    15L      // 75점
            );

            // when
            Double bestScore = decimalData.calculateBestScore();

            // then
            // 실제 계산: (75*0.4) + (75*0.3) + (75*0.15) + (74*0.1) + (75*0.05) = 30 + 22.5 + 11.25 + 7.4 + 3.75 = 74.9
            assertThat(bestScore).isEqualTo(74.9);
        }

        @Test
        @DisplayName("베스트 점수 계산 - 기준치 초과값 처리")
        void calculateBestScore_ExceedsMaxReference() {
            // given - 모든 값이 기준치를 크게 초과
            ProductBestScoreData exceedsData = new ProductBestScoreData(
                    "product1",
                    500L,     // 기준치(100)의 5배
                    5000000L, // 기준치(100만)의 5배
                    5.0,      // 최대값
                    250L,     // 기준치(50)의 5배
                    100L      // 기준치(20)의 5배
            );

            // when
            Double bestScore = exceedsData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(100.0); // 최대 100점으로 제한
        }

        @Test
        @DisplayName("베스트 점수 계산 - 빈 데이터 객체")
        void calculateBestScore_EmptyData() {
            // given
            ProductBestScoreData emptyData = ProductBestScoreData.empty("product1");

            // when
            Double bestScore = emptyData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(0.0);
            assertThat(emptyData.productId()).isEqualTo("product1");
            assertThat(emptyData.salesQuantity()).isEqualTo(0L);
            assertThat(emptyData.totalRevenue()).isEqualTo(0L);
            assertThat(emptyData.avgRating()).isEqualTo(0.0);
            assertThat(emptyData.reviewCount()).isEqualTo(0L);
            assertThat(emptyData.recentOrderCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("캐시 동작 테스트")
    class CacheTests {

        @Test
        @DisplayName("캐시 키 생성 확인")
        void getProductBestScoreData_CacheKey_Verification() {
            // given
            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(testScoreData);

            // when
            productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            // @Cacheable 어노테이션의 key = "#sellerId"
            // 캐시 키: "2aa4ad9f-dd05-4739-a683-eb8d2115635f"
            verify(productBestScoreMapper).getProductBestScoreDataBySeller(testSellerId);
        }

        @Test
        @DisplayName("다른 판매자 ID로 캐시 키 분리 확인")
        void getProductBestScoreData_DifferentSellerCache() {
            // given
            String anotherSellerId = "different-seller-id";
            List<ProductBestScoreData> anotherData = List.of(
                    new ProductBestScoreData("product4", 60L, 600000L, 3.8, 40L, 12L)
            );

            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(testScoreData);
            given(productBestScoreMapper.getProductBestScoreDataBySeller(anotherSellerId))
                    .willReturn(anotherData);

            // when
            List<ProductBestScoreData> result1 = productBestScoreService.getProductBestScoreData(testSellerId);
            List<ProductBestScoreData> result2 = productBestScoreService.getProductBestScoreData(anotherSellerId);

            // then
            assertThat(result1).hasSize(3);
            assertThat(result2).hasSize(1);
            assertThat(result1.get(0).productId()).isEqualTo("product1");
            assertThat(result2.get(0).productId()).isEqualTo("product4");

            verify(productBestScoreMapper).getProductBestScoreDataBySeller(testSellerId);
            verify(productBestScoreMapper).getProductBestScoreDataBySeller(anotherSellerId);
        }
    }

    @Nested
    @DisplayName("데이터 정렬 및 순서 테스트")
    class DataOrderingTests {

        @Test
        @DisplayName("베스트 점수 기준 정렬 확인")
        void getProductBestScoreData_SortingByBestScore() {
            // given
            List<ProductBestScoreData> unsortedData = Arrays.asList(
                    new ProductBestScoreData("product1", 50L, 500000L, 3.0, 30L, 10L),   // 낮은 점수
                    new ProductBestScoreData("product2", 100L, 1000000L, 5.0, 50L, 20L), // 높은 점수
                    new ProductBestScoreData("product3", 75L, 750000L, 4.0, 40L, 15L)    // 중간 점수
            );

            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(unsortedData);

            // when
            List<ProductBestScoreData> result = productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            assertThat(result).hasSize(3);

            // 매퍼에서 반환된 순서 그대로 유지 (정렬은 상위 서비스에서 처리)
            assertThat(result.get(0).productId()).isEqualTo("product1");
            assertThat(result.get(1).productId()).isEqualTo("product2");
            assertThat(result.get(2).productId()).isEqualTo("product3");

            // 각 상품의 베스트 점수 계산 확인
            Double score1 = result.get(0).calculateBestScore();
            Double score2 = result.get(1).calculateBestScore();
            Double score3 = result.get(2).calculateBestScore();

            assertThat(score2).isGreaterThan(score3); // product2 > product3
            assertThat(score3).isGreaterThan(score1); // product3 > product1
        }

        @Test
        @DisplayName("대량 데이터 처리 확인")
        void getProductBestScoreData_LargeDataSet() {
            // given - 많은 상품 데이터
            List<ProductBestScoreData> largeDataSet = Arrays.asList(
                    new ProductBestScoreData("product1", 10L, 100000L, 4.1, 20L, 5L),
                    new ProductBestScoreData("product2", 20L, 200000L, 4.2, 25L, 8L),
                    new ProductBestScoreData("product3", 30L, 300000L, 4.3, 30L, 10L),
                    new ProductBestScoreData("product4", 40L, 400000L, 4.4, 35L, 12L),
                    new ProductBestScoreData("product5", 50L, 500000L, 4.5, 40L, 15L)
            );

            given(productBestScoreMapper.getProductBestScoreDataBySeller(testSellerId))
                    .willReturn(largeDataSet);

            // when
            List<ProductBestScoreData> result = productBestScoreService.getProductBestScoreData(testSellerId);

            // then
            assertThat(result).hasSize(5);
            assertThat(result.get(0).productId()).isEqualTo("product1");
            assertThat(result.get(4).productId()).isEqualTo("product5");

            // 모든 데이터가 올바르게 반환되는지 확인
            for (int i = 0; i < result.size(); i++) {
                ProductBestScoreData data = result.get(i);
                assertThat(data.productId()).isEqualTo("product" + (i + 1));
                assertThat(data.calculateBestScore()).isGreaterThanOrEqualTo(0.0);
            }
        }
    }
}