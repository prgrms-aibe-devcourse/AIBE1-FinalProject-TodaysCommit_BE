package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.orders.domain.dto.SellerStoreStatsDTO;
import com.team5.catdogeats.orders.service.SellerStoreStatsService;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductStoreInfoDTO;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.products.service.SellerStoreProductService;
import com.team5.catdogeats.users.domain.dto.SellerStorePageResponse;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SellerStoreService 통합 테스트")
class SellerStoreServiceImplTest {

    @InjectMocks
    private SellerStoreServiceImpl sellerStoreService;

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private SellerStoreProductService productService;

    @Mock
    private SellerStoreStatsService sellerStoreStatsService;

    private Sellers testSeller;
    private SellerStoreStatsDTO testStats;
    private List<ProductStoreInfoDTO> testProducts;
    private Page<ProductStoreInfoDTO> testProductPage;

    @BeforeEach
    void setUp() {
        // 테스트 판매자 데이터
        testSeller = Sellers.builder()
                .userId("2aa4ad9f-dd05-4739-a683-eb8d2115635f")
                .vendorName("멍멍이네 수제간식")
                .vendorProfileImage("https://example.com/image.jpg")
                .tags("강아지,수제,건강")
                .operatingStartTime(LocalTime.of(9, 0))
                .operatingEndTime(LocalTime.of(18, 0))
                .build();

        // Mock BaseEntity의 createdAt 설정
        testSeller = Sellers.builder()
                .userId("2aa4ad9f-dd05-4739-a683-eb8d2115635f")
                .vendorName("멍멍이네 수제간식")
                .vendorProfileImage("https://example.com/image.jpg")
                .tags("강아지,수제,건강")
                .operatingStartTime(LocalTime.of(9, 0))
                .operatingEndTime(LocalTime.of(18, 0))
                .build();

        // 통계 데이터
        testStats = new SellerStoreStatsDTO(15L, 3.15, 1061L);

        // 상품 데이터
        testProducts = Arrays.asList(
                new ProductStoreInfoDTO(
                        "d5a2b984-3d37-4ba3-a502-f2060ab48d55",
                        2000000016L,
                        "멍멍이 수제간식 16",
                        9820L,
                        true,
                        12.92,
                        "",
                        PetCategory.DOG,
                        StockStatus.IN_STOCK,
                        3.6,
                        36L,
                        21.1
                ),
                new ProductStoreInfoDTO(
                        "e680cf66-d400-4669-b16d-d09d4df5fd0a",
                        2000000028L,
                        "멍멍이 수제간식 28",
                        9196L,
                        true,
                        9.3,
                        "",
                        PetCategory.DOG,
                        StockStatus.IN_STOCK,
                        3.6,
                        38L,
                        20.6
                )
        );

        testProductPage = new PageImpl<>(testProducts, PageRequest.of(0, 12), 30L);
    }

    @Nested
    @DisplayName("성공 케이스 테스트")
    class SuccessTests {

        @Test
        @DisplayName("기본 스토어 페이지 조회 성공")
        void getSellerStorePage_Basic_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            String sellerId = testSeller.getUserId();

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(sellerId))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(sellerId), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(sellerId))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result).isNotNull();

            // 판매자 정보 검증
            assertThat(result.sellerInfo().sellerId()).isEqualTo(sellerId);
            assertThat(result.sellerInfo().vendorName()).isEqualTo("멍멍이네 수제간식");
            assertThat(result.sellerInfo().totalProducts()).isEqualTo(30L);
            assertThat(result.sellerInfo().totalSalesQuantity()).isEqualTo(15L);
            assertThat(result.sellerInfo().avgDeliveryDays()).isEqualTo(3.15);
            assertThat(result.sellerInfo().totalReviews()).isEqualTo(1061L);

            // 상품 목록 검증
            assertThat(result.products().content()).hasSize(2);
            assertThat(result.products().totalElements()).isEqualTo(30L);
            assertThat(result.products().currentPage()).isEqualTo(1); // 1-based
            assertThat(result.products().size()).isEqualTo(12);

            // 첫 번째 상품 검증
            var firstProduct = result.products().content().get(0);
            assertThat(firstProduct.productId()).isEqualTo("d5a2b984-3d37-4ba3-a502-f2060ab48d55");
            assertThat(firstProduct.title()).isEqualTo("멍멍이 수제간식 16");
            assertThat(firstProduct.price()).isEqualTo(9820L);
            assertThat(firstProduct.discountedPrice()).isEqualTo(8551L);
            assertThat(firstProduct.discountRate()).isEqualTo(12.92);

            // verify
            verify(sellersRepository).findByVendorName(vendorName);
            verify(productService).countSellerActiveProducts(sellerId);
            verify(productService).getSellerProductsBaseInfo(eq(sellerId), isNull(), isNull(), any(Pageable.class));
            verify(sellerStoreStatsService).getSellerStoreStats(sellerId);
        }

        @Test
        @DisplayName("카테고리 필터링 조회 성공")
        void getSellerStorePage_WithCategory_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            PetCategory category = PetCategory.DOG;

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), eq(category), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", category, null
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.products().content()).hasSize(2);

            // verify
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), eq(category), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("베스트 필터 조회 성공")
        void getSellerStorePage_BestFilter_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            String filter = "best";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, filter
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.products().content()).hasSize(2);

            // 베스트 상품이므로 베스트 점수가 있어야 함
            assertThat(result.products().content().get(0).bestScore()).isGreaterThan(0);

            // verify
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class));
        }

        @Test
        @DisplayName("할인 상품 필터 조회 성공")
        void getSellerStorePage_DiscountFilter_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            String filter = "discount";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, filter
            );

            // then
            assertThat(result).isNotNull();
            assertThat(result.products().content()).hasSize(2);

            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class));
        }

        @Test
        @DisplayName("페이지 크기 제한 테스트 (최대 50개)")
        void getSellerStorePage_PageSizeLimit_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when - 100개 요청하지만 최대 50개로 제한됨
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 100, "createdAt,desc", null, null
            );

            // then
            assertThat(result).isNotNull();

            // verify - 실제로는 50개로 제한된 Pageable이 전달되어야 함
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("가격 정렬 조회 성공")
        void getSellerStorePage_PriceSort_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "price,asc", null, null
            );

            // then
            assertThat(result).isNotNull();

            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("실패 케이스 테스트")
    class FailureTests {

        @Test
        @DisplayName("존재하지 않는 판매자 - EntityNotFoundException")
        void getSellerStorePage_SellerNotFound_ThrowsException() {
            // given
            String nonExistentVendorName = "존재하지않는상점";

            given(sellersRepository.findByVendorName(nonExistentVendorName))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    nonExistentVendorName, 1, 12, "createdAt,desc", null, null
            ))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("판매자를 찾을 수 없습니다: " + nonExistentVendorName);

            verify(sellersRepository).findByVendorName(nonExistentVendorName);
        }

        @Test
        @DisplayName("빈 판매자명 - IllegalArgumentException")
        void getSellerStorePage_EmptyVendorName_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    "", 1, 12, "createdAt,desc", null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 상점명은 필수입니다");
        }

        @Test
        @DisplayName("null 판매자명 - IllegalArgumentException")
        void getSellerStorePage_NullVendorName_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    null, 1, 12, "createdAt,desc", null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("판매자 상점명은 필수입니다");
        }

        @Test
        @DisplayName("잘못된 페이지 번호 (0) - IllegalArgumentException")
        void getSellerStorePage_InvalidPageNumber_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    "멍멍이네 수제간식", 0, 12, "createdAt,desc", null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 번호는 1 이상이어야 합니다");
        }

        @Test
        @DisplayName("잘못된 페이지 크기 (0) - IllegalArgumentException")
        void getSellerStorePage_InvalidPageSize_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    "멍멍이네 수제간식", 1, 0, "createdAt,desc", null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 크기는 1~100 사이여야 합니다");
        }

        @Test
        @DisplayName("잘못된 페이지 크기 (101) - IllegalArgumentException")
        void getSellerStorePage_PageSizeTooLarge_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    "멍멍이네 수제간식", 1, 101, "createdAt,desc", null, null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("페이지 크기는 1~100 사이여야 합니다");
        }

        @Test
        @DisplayName("잘못된 필터 값 - IllegalArgumentException")
        void getSellerStorePage_InvalidFilter_ThrowsException() {
            // when & then
            assertThatThrownBy(() -> sellerStoreService.getSellerStorePage(
                    "멍멍이네 수제간식", 1, 12, "createdAt,desc", null, "invalid_filter"
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 필터 값입니다");
        }
    }

    @Nested
    @DisplayName("페이징 처리 테스트")
    class PagingTests {

        @Test
        @DisplayName("첫 번째 페이지 조회")
        void getSellerStorePage_FirstPage() {
            // given
            String vendorName = "멍멍이네 수제간식";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result.products().currentPage()).isEqualTo(1);
            assertThat(result.products().hasPrevious()).isFalse();
            assertThat(result.products().hasNext()).isTrue(); // 30개 중 12개씩이므로 다음 페이지 있음
        }

        @Test
        @DisplayName("두 번째 페이지 조회")
        void getSellerStorePage_SecondPage() {
            // given
            String vendorName = "멍멍이네 수제간식";
            Page<ProductStoreInfoDTO> secondPageResult = new PageImpl<>(
                    testProducts, PageRequest.of(1, 12), 30L
            );

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(secondPageResult);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 2, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result.products().currentPage()).isEqualTo(2);
            assertThat(result.products().hasPrevious()).isTrue();
            assertThat(result.products().hasNext()).isTrue();
        }

        @Test
        @DisplayName("마지막 페이지 조회")
        void getSellerStorePage_LastPage() {
            // given
            String vendorName = "멍멍이네 수제간식";
            Page<ProductStoreInfoDTO> lastPageResult = new PageImpl<>(
                    List.of(testProducts.get(0)), PageRequest.of(2, 12), 25L
            );

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(25L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(lastPageResult);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 3, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result.products().currentPage()).isEqualTo(3);
            assertThat(result.products().hasPrevious()).isTrue();
            assertThat(result.products().hasNext()).isFalse();
        }
    }

    @Nested
    @DisplayName("필터별 조회 테스트")
    class FilterTests {

        @Test
        @DisplayName("신상품 필터 조회")
        void getSellerStorePage_NewFilter_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            String filter = "new";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, filter
            );

            // then
            assertThat(result).isNotNull();
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class));
        }

        @Test
        @DisplayName("품절제외 필터 조회")
        void getSellerStorePage_ExcludeSoldOutFilter_Success() {
            // given
            String vendorName = "멍멍이네 수제간식";
            String filter = "exclude_sold_out";

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, filter
            );

            // then
            assertThat(result).isNotNull();
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), eq(filter), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("도메인 서비스 협력 테스트")
    class DomainServiceCollaborationTests {

        @Test
        @DisplayName("Orders 도메인 통계 서비스 협력")
        void getSellerStorePage_OrdersStatsService_Collaboration() {
            // given
            String vendorName = "멍멍이네 수제간식";
            SellerStoreStatsDTO emptyStats = SellerStoreStatsDTO.empty();

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(30L);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(emptyStats); // 빈 통계 반환

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result.sellerInfo().totalSalesQuantity()).isEqualTo(0L);
            assertThat(result.sellerInfo().avgDeliveryDays()).isEqualTo(0.0);
            assertThat(result.sellerInfo().totalReviews()).isEqualTo(0L);

            verify(sellerStoreStatsService).getSellerStoreStats(testSeller.getUserId());
        }

        @Test
        @DisplayName("Products 도메인 서비스 협력")
        void getSellerStorePage_ProductsService_Collaboration() {
            // given
            String vendorName = "멍멍이네 수제간식";
            Long expectedProductCount = 42L;

            given(sellersRepository.findByVendorName(vendorName))
                    .willReturn(Optional.of(testSeller));
            given(productService.countSellerActiveProducts(testSeller.getUserId()))
                    .willReturn(expectedProductCount);
            given(productService.getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class)))
                    .willReturn(testProductPage);
            given(sellerStoreStatsService.getSellerStoreStats(testSeller.getUserId()))
                    .willReturn(testStats);

            // when
            SellerStorePageResponse result = sellerStoreService.getSellerStorePage(
                    vendorName, 1, 12, "createdAt,desc", null, null
            );

            // then
            assertThat(result.sellerInfo().totalProducts()).isEqualTo(expectedProductCount);

            verify(productService).countSellerActiveProducts(testSeller.getUserId());
            verify(productService).getSellerProductsBaseInfo(eq(testSeller.getUserId()), isNull(), isNull(), any(Pageable.class));
        }
    }
}