package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.carts.domain.Carts;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("CartItemRepository 테스트")
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SellersRepository sellersRepository;

    private Users testUser;
    private Users testSeller;
    private Sellers seller;
    private Carts testCart;
    private Products testProduct1;
    private Products testProduct2;

    @BeforeEach
    void setUp() {
        // 테스트용 구매자 생성
        testUser = Users.builder()
                .provider("google")
                .providerId("test-buyer-id")
                .userNameAttribute("sub")
                .name("테스트 구매자")
                .role(Role.ROLE_BUYER)
                .build();
        testUser = userRepository.save(testUser);

        // 테스트용 판매자 생성
        testSeller = Users.builder()
                .provider("google")
                .providerId("test-seller-id")
                .userNameAttribute("sub")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .build();
        testSeller = userRepository.save(testSeller);

        // 판매자 정보 생성 - entityManager를 사용하여 flush 없이 저장
        seller = Sellers.builder()
                .userId(testSeller.getId())
                .user(testSeller)
                .vendorName("테스트 상점")
                .build();
        entityManager.persistAndFlush(seller);

        // 테스트용 장바구니 생성
        testCart = Carts.builder()
                .user(testUser)
                .build();
        testCart = cartRepository.save(testCart);

        // 테스트용 상품 생성 - entityManager를 사용
        testProduct1 = Products.builder()
                .productNumber(1001L)
                .seller(seller)
                .title("테스트 상품 1")
                .contents("테스트 상품 1 설명")
                .petCategory(PetCategory.DOG)
                .productCategory(ProductCategory.HANDMADE)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(false)
                .price(10000L)
                .leadTime((short) 3)
                .stock(100)
                .build();
        entityManager.persistAndFlush(testProduct1);

        testProduct2 = Products.builder()
                .productNumber(1002L)
                .seller(seller)
                .title("테스트 상품 2")
                .contents("테스트 상품 2 설명")
                .petCategory(PetCategory.CAT)
                .productCategory(ProductCategory.FINISHED)
                .stockStatus(StockStatus.IN_STOCK)
                .isDiscounted(true)
                .discountRate(10.0)
                .price(15000L)
                .leadTime((short) 5)
                .stock(50)
                .build();
        entityManager.persistAndFlush(testProduct2);
    }

    @Test
    @DisplayName("장바구니 ID로 장바구니 아이템 조회")
    void findByCartsId() {
        // given
        CartItems cartItem1 = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        CartItems cartItem2 = CartItems.builder()
                .carts(testCart)
                .product(testProduct2)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);

        // when
        List<CartItems> cartItems = cartItemRepository.findByCartsId(testCart.getId());

        // then
        assertThat(cartItems).hasSize(2);
        assertThat(cartItems).extracting(CartItems::getQuantity)
                .containsExactlyInAnyOrder(2, 1);
    }

    @Test
    @DisplayName("장바구니 ID와 상품 ID로 장바구니 아이템 조회 - 성공")
    void findByCartsIdAndProductId_Success() {
        // given
        CartItems cartItem = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(3)
                .build();
        cartItemRepository.save(cartItem);

        // when
        Optional<CartItems> foundItem = cartItemRepository
                .findByCartsIdAndProductId(testCart.getId(), testProduct1.getId());

        // then
        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getQuantity()).isEqualTo(3);
        assertThat(foundItem.get().getProduct().getId()).isEqualTo(testProduct1.getId());
    }

    @Test
    @DisplayName("장바구니 ID와 상품 ID로 장바구니 아이템 조회 - 존재하지 않음")
    void findByCartsIdAndProductId_NotFound() {
        // when
        Optional<CartItems> foundItem = cartItemRepository
                .findByCartsIdAndProductId(testCart.getId(), "nonexistent-product-id");

        // then
        assertThat(foundItem).isEmpty();
    }

    @Test
    @DisplayName("장바구니 ID로 상품 정보와 함께 장바구니 아이템 조회 (JOIN FETCH)")
    void findByCartsIdWithProduct() {
        // given
        CartItems cartItem = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        cartItemRepository.save(cartItem);

        // when
        List<CartItems> cartItems = cartItemRepository.findByCartsIdWithProduct(testCart.getId());

        // then
        assertThat(cartItems).hasSize(1);
        assertThat(cartItems.get(0).getProduct()).isNotNull();
        assertThat(cartItems.get(0).getProduct().getTitle()).isEqualTo("테스트 상품 1");
        assertThat(cartItems.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("장바구니 ID로 아이템 개수 조회")
    void countByCartsId() {
        // given
        CartItems cartItem1 = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        CartItems cartItem2 = CartItems.builder()
                .carts(testCart)
                .product(testProduct2)
                .quantity(1)
                .build();
        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);

        // when
        long count = cartItemRepository.countByCartsId(testCart.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("장바구니 ID와 아이템 ID로 장바구니 아이템 삭제")
    void deleteByCartsIdAndId() {
        // given
        CartItems cartItem = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        CartItems savedItem = cartItemRepository.save(cartItem);

        // when
        cartItemRepository.deleteByCartsIdAndId(testCart.getId(), savedItem.getId());

        // then
        Optional<CartItems> deletedItem = cartItemRepository.findById(savedItem.getId());
        assertThat(deletedItem).isEmpty();
    }

    @Test
    @DisplayName("빈 장바구니에서 아이템 조회")
    void findByCartsId_EmptyCart() {
        // when
        List<CartItems> cartItems = cartItemRepository.findByCartsId(testCart.getId());

        // then
        assertThat(cartItems).isEmpty();
    }

    @Test
    @DisplayName("빈 장바구니에서 아이템 개수 조회")
    void countByCartsId_EmptyCart() {
        // when
        long count = cartItemRepository.countByCartsId(testCart.getId());

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("장바구니 아이템 저장 및 조회")
    void saveAndFind() {
        // given
        CartItems cartItem = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(5)
                .build();

        // when
        CartItems savedItem = cartItemRepository.save(cartItem);
        Optional<CartItems> foundItem = cartItemRepository.findById(savedItem.getId());

        // then
        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getId()).isEqualTo(savedItem.getId());
        assertThat(foundItem.get().getQuantity()).isEqualTo(5);
        assertThat(foundItem.get().getCarts().getId()).isEqualTo(testCart.getId());
        assertThat(foundItem.get().getProduct().getId()).isEqualTo(testProduct1.getId());
    }

    @Test
    @DisplayName("같은 상품을 다른 장바구니에 추가")
    void addSameProductToDifferentCarts() {
        // given
        Users anotherUser = Users.builder()
                .provider("google")
                .providerId("another-user-id")
                .userNameAttribute("sub")
                .name("다른 사용자")
                .role(Role.ROLE_BUYER)
                .build();
        anotherUser = userRepository.save(anotherUser);

        Carts anotherCart = Carts.builder()
                .user(anotherUser)
                .build();
        anotherCart = cartRepository.save(anotherCart);

        CartItems cartItem1 = CartItems.builder()
                .carts(testCart)
                .product(testProduct1)
                .quantity(2)
                .build();
        CartItems cartItem2 = CartItems.builder()
                .carts(anotherCart)
                .product(testProduct1)
                .quantity(3)
                .build();

        // when
        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);

        // then
        List<CartItems> firstCartItems = cartItemRepository.findByCartsId(testCart.getId());
        List<CartItems> secondCartItems = cartItemRepository.findByCartsId(anotherCart.getId());

        assertThat(firstCartItems).hasSize(1);
        assertThat(firstCartItems.get(0).getQuantity()).isEqualTo(2);
        assertThat(secondCartItems).hasSize(1);
        assertThat(secondCartItems.get(0).getQuantity()).isEqualTo(3);
    }
}