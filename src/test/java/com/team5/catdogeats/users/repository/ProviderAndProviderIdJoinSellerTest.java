package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DataJpaTest
@DisplayName("Users와 Sellers에 사용되는 join 쿼리 테스트")
public class ProviderAndProviderIdJoinSellerTest {
    @Autowired
    private SellersRepository sellersRepository;

    @Autowired
    private TestEntityManager em;

    Users user;
    Sellers seller;

    @BeforeEach
    void setUp() {
        user = Users.builder()
                .provider("kakao")
                .providerId("kakao-456")
                .userNameAttribute("sub")
                .name("김판매")
                .role(Role.ROLE_SELLER)
                .build();
        em.persist(user);

        seller = Sellers.builder()
                .user(user)
                .vendorName("맛있는상점")
                .vendorProfileImage("https://example.com/profile.png")
                .businessNumber("123-45-67890")
                .settlementBank("국민은행")
                .settlementAccount("123-456-7890")
                .tags("한식,분식")
                .operatingStartTime(LocalTime.of(9, 0))
                .operatingEndTime(LocalTime.of(21, 0))
                .closedDays("월요일")
                .build();
        em.persist(seller);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("provider/providerId로 SellerDTO를 조회")
    void testFindSellerByProviderAndProviderId() {
        Optional<SellerDTO> result = sellersRepository
                .findSellerDtoByProviderAndProviderId("kakao", "kakao-456");

        assertThat(result).isPresent();
        SellerDTO dto = result.get();
        assertThat(dto.userId()).isEqualTo(user.getId());
        assertThat(dto.vendorName()).isEqualTo("맛있는상점");
        assertThat(dto.businessNumber()).isEqualTo("123-45-67890");
    }
}
