package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.orders.service.ProductBestScoreService;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductBestScoreDataDTO;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfoDTO;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.products.mapper.ProductStoreMapper;
import com.team5.catdogeats.users.controller.SellerStoreExceptionHandler.ProductDataRetrievalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerStoreProductService 단위 테스트")
class SellerStoreProductServiceImplTest {

    @InjectMocks
    private SellerStoreProductServiceImpl sellerStoreProductService;


    @Mock
    private ProductStoreMapper productStoreMapper;

    @Mock
    private ProductBestScoreService productBestScoreService;

    private String testSellerId;
    private List<ProductStoreInfoDTO> testProducts;
    private List<ProductBestScoreDataDTO> testBestScoreData;

    @BeforeEach
    void setUp() {
        testSellerId = "2aa4ad9f-dd05-4739-a683-eb8d2115635f";

        testProducts = Arrays.asList(
                new ProductStoreInfoDTO(
                        "product1",
                        1001L,
                        "상품1",
                        10000L,
                        true,
                        10.0,
                        "image1.jpg",
                        PetCategory.DOG,
                        ProductCategory.HANDMADE,
                        StockStatus.IN_STOCK,
                        4.5,
                        100L,
                        85.5
                ),
                new ProductStoreInfoDTO(
                        "product2",
                        1002L,
                        "상품2",
                        20000L,
                        false,
                        0.0,
                        "image2.jpg",
                        PetCategory.CAT,
                        ProductCategory.FINISHED,
                        StockStatus.LOW_STOCK,
                        4.0,
                        50L,
                        75.0
                )
        );

        testBestScoreData = Arrays.asList(
                new ProductBestScoreDataDTO("product1", 100L, 1000000L, 4.5, 100L, 30L),
                new ProductBestScoreDataDTO("product2", 80L, 800000L, 4.0, 50L, 20L)
        );
    }

    @Nested
    @DisplayName("일반 상품 조회 테스트")
    class GeneralProductQueryTests {

        @Test
        @DisplayName("성공 - 기본 상품 목록 조회")
        void getSellerProductsBaseInfo_Basic_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 12);
            given(productStoreMapper.findSellerProductsBaseInfo(testSellerId, null, null,null, 12, 0))
                    .willReturn(testProducts);
            given(productStoreMapper.countSellerProductsForStore(testSellerId, null,null, null))
                    .willReturn(50L);

            // when
            Page<ProductStoreInfoDTO> result = sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null, null, null,pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(50L);
            assertThat(result.getContent().get(0).productId()).isEqualTo("product1");
            assertThat(result.getContent().get(1).productId()).isEqualTo("product2");

            verify(productStoreMapper).findSellerProductsBaseInfo(testSellerId, null, null, null,12, 0);
            verify(productStoreMapper).countSellerProductsForStore(testSellerId, null, null,null);
        }

        @Test
        @DisplayName("성공 - 카테고리 필터 적용")
        void getSellerProductsBaseInfo_WithCategory_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 12);
            PetCategory category = PetCategory.DOG;
            given(productStoreMapper.findSellerProductsBaseInfo(testSellerId, "DOG", null, null,12, 0))
                    .willReturn(List.of(testProducts.get(0))); // DOG 상품만
            given(productStoreMapper.countSellerProductsForStore(testSellerId, "DOG", null,null))
                    .willReturn(25L);

            // when
            Page<ProductStoreInfoDTO> result = sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, category, null,null, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).petCategory()).isEqualTo(PetCategory.DOG);
            assertThat(result.getTotalElements()).isEqualTo(25L);

            verify(productStoreMapper).findSellerProductsBaseInfo(testSellerId, "DOG", null, null,12, 0);
        }

        @Test
        @DisplayName("성공 - 할인 필터 적용")
        void getSellerProductsBaseInfo_DiscountFilter_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 12);
            String filter = "discount";
            given(productStoreMapper.findSellerProductsBaseInfo(testSellerId, null, null, filter, 12, 0))
                    .willReturn(List.of(testProducts.get(0))); // 할인 상품만
            given(productStoreMapper.countSellerProductsForStore(testSellerId, null,null, filter))
                    .willReturn(10L);

            // when
            Page<ProductStoreInfoDTO> result = sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null,null, filter, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).isDiscounted()).isTrue();

            verify(productStoreMapper).findSellerProductsBaseInfo(testSellerId, null,null, filter, 12, 0);
        }

        @Test
        @DisplayName("실패 - 빈 판매자 ID")
        void getSellerProductsBaseInfo_EmptySellerId_ThrowsException() {
            // given
            Pageable pageable = PageRequest.of(0, 12);

            // when & then
            assertThatThrownBy(() -> sellerStoreProductService
                    .getSellerProductsBaseInfo("", null, null, null,pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 ID는 필수입니다");
        }

        @Test
        @DisplayName("실패 - 페이지 크기 초과")
        void getSellerProductsBaseInfo_PageSizeExceeded_ThrowsException() {
            // given
            Pageable pageable = PageRequest.of(0, 101); // 100 초과

            // when & then
            assertThatThrownBy(() -> sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null, null,null, pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 크기는 100을 초과할 수 없습니다");
        }

        @Test
        @DisplayName("실패 - 잘못된 필터 값")
        void getSellerProductsBaseInfo_InvalidFilter_ThrowsException() {
            // given
            Pageable pageable = PageRequest.of(0, 12);

            // when & then
            assertThatThrownBy(() -> sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null,null, "invalid_filter", pageable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 필터 값");
        }

        @Test
        @DisplayName("실패 - 데이터 조회 실패")
        void getSellerProductsBaseInfo_DataRetrievalFailure_ThrowsException() {
            // given
            Pageable pageable = PageRequest.of(0, 12);
            given(productStoreMapper.findSellerProductsBaseInfo(anyString(), any(), any(), any(),anyInt(), anyInt()))
                    .willThrow(new RuntimeException("DB 연결 실패"));

            // when & then
            assertThatThrownBy(() -> sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null, null,null, pageable))
                    .isInstanceOf(ProductDataRetrievalException.class)
                    .hasMessageContaining("상품 정보 조회 실패");
        }
    }

    @Nested
    @DisplayName("베스트 상품 조회 테스트")
    class BestProductQueryTests {

        @Test
        @DisplayName("성공 - 베스트 상품 조회")
        void getSellerProductsBaseInfo_BestFilter_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            List<String> topProductIds = Arrays.asList("product1", "product2");

            given(productBestScoreService.getProductBestScoreData(testSellerId))
                    .willReturn(testBestScoreData);
            given(productStoreMapper.findProductsByIds(topProductIds,null,null))
                    .willReturn(testProducts);

            // when
            Page<ProductStoreInfoDTO> result = sellerStoreProductService
                    .getSellerProductsBaseInfo(testSellerId, null, null,"best", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2L);

            // 베스트 점수 계산 공식:
            // (판매량*0.4) + (매출액*0.3) + (평점*0.15) + (리뷰수*0.1) + (최근주문*0.05)
            // 각 지표는 0-100으로 정규화됨
        }

        @Test
        @DisplayName("베스트 점수 계산 - 빈 데이터")
        void calculateBestScore_EmptyData() {
            // given
            ProductBestScoreDataDTO emptyData = ProductBestScoreDataDTO.empty("product1");

            // when
            Double bestScore = emptyData.calculateBestScore();

            // then
            assertThat(bestScore).isEqualTo(0.0);
        }

        @Test
        @DisplayName("베스트 점수 계산 - 높은 성과 상품")
        void calculateBestScore_HighPerformanceProduct() {
            // given
            ProductBestScoreDataDTO highPerformanceData = new ProductBestScoreDataDTO(
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
            assertThat(bestScore).isEqualTo(100.0); // 모든 지표가 만점
        }
    }
}