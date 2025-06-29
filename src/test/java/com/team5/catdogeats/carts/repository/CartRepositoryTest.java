package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.carts.domain.Carts;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("CartRepository 테스트")
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        testUser = Users.builder()
                .provider("google")
                .providerId("test-provider-id")
                .userNameAttribute("sub")
                .name("테스트 사용자")
                .role(Role.ROLE_BUYER)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 조회 - 성공")
    void findByUserId_Success() {
        // given
        Carts cart = Carts.builder()
                .user(testUser)
                .build();
        cartRepository.save(cart);

        // when
        Optional<Carts> foundCart = cartRepository.findByUserId(testUser.getId());

        // then
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 존재 여부 확인 - 존재함")
    void existsByUserId_True() {
        // given
        Carts cart = Carts.builder()
                .user(testUser)
                .build();
        cartRepository.save(cart);

        // when
        boolean exists = cartRepository.existsByUserId(testUser.getId());

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("사용자 ID로 장바구니 존재 여부 확인 - 존재하지 않음")
    void existsByUserId_False() {
        // when
        boolean notExists = cartRepository.existsByUserId("nonexistent-user-id");

        // then
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 장바구니 조회")
    void findByUserId_NotFound() {
        // when
        Optional<Carts> foundCart = cartRepository.findByUserId("nonexistent-user-id");

        // then
        assertThat(foundCart).isEmpty();
    }

    @Test
    @DisplayName("사용자가 여러 장바구니를 가질 수 없음 - 유니크 제약조건 테스트")
    void findByUserId_UniqueConstraint() {
        // given
        Carts firstCart = Carts.builder()
                .user(testUser)
                .build();
        cartRepository.save(firstCart);

        // when
        Optional<Carts> foundCart = cartRepository.findByUserId(testUser.getId());

        // then
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("장바구니 저장 및 조회")
    void saveAndFind() {
        // given
        Carts cart = Carts.builder()
                .user(testUser)
                .build();

        // when
        Carts savedCart = cartRepository.save(cart);
        Optional<Carts> foundCart = cartRepository.findById(savedCart.getId());

        // then
        assertThat(foundCart).isPresent();
        assertThat(foundCart.get().getId()).isEqualTo(savedCart.getId());
        assertThat(foundCart.get().getUser().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("장바구니 삭제")
    void deleteCart() {
        // given
        Carts cart = Carts.builder()
                .user(testUser)
                .build();
        Carts savedCart = cartRepository.save(cart);

        // when
        cartRepository.delete(savedCart);
        Optional<Carts> deletedCart = cartRepository.findById(savedCart.getId());

        // then
        assertThat(deletedCart).isEmpty();
    }
}