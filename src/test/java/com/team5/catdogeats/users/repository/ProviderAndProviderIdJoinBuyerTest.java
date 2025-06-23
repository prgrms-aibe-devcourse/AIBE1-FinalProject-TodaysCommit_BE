package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@DisplayName("Users와 Buyers에 사용되는 join 쿼리 테스트")
public class ProviderAndProviderIdJoinBuyerTest {
    @Autowired
    private BuyerRepository buyersRepository;

    @Autowired
    private TestEntityManager em;

    private Users user;
    private Buyers buyer;

    @BeforeEach
    void setUp() {
        user = Users.builder()
                .provider("google")
                .providerId("google-123")
                .userNameAttribute("sub")
                .name("홍길동")
                .role(Role.ROLE_BUYER)
                .build();
        em.persist(user);

        buyer = Buyers.builder()
                .user(user)
                .nameMaskingStatus(true)
                .build();
        em.persist(buyer);

        em.flush();
        em.clear(); // 1차 캐시 초기화 (진짜 쿼리 나가는지 확인)
    }

    @Test
    @DisplayName("provider와 providerId로 Buyers를 조인해서 조회한다")
    void testFindBuyersByProviderAndProviderId() {
        // given: Users + Buyers 엔티티 저장

        // when: DTO 프로젝션 조회
        Optional<BuyerDTO> optDto = buyersRepository
                .findOnlyBuyerByProviderAndProviderId("google", "google-123");

        // then: 결과 검증
        assertThat(optDto).isPresent();
        BuyerDTO dto = optDto.get();
        assertThat(dto.userId()).isEqualTo(user.getId());
        assertThat(dto.nameMaskingStatus()).isTrue();
    }
}
