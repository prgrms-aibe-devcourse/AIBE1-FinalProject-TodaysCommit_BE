package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.dto.WithdrawBuyerDTO;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@Transactional
class UserWithdrawMapperTest {

    @Autowired
    private UserWithdrawMapper mapper;

    @Autowired
    private TestMapper testMapper;

    @Autowired
    private EntityManager em;

    private Users user1;
    private Users user2;
    private final OffsetDateTime deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
    private Buyers buyer;
    private Sellers seller;


    @BeforeEach
    void setup() {
         user1 = Users.builder()
                .provider("google")
                .providerId("google-123")
                .userNameAttribute("sub")
                .name("test")
                .accountDisable(true)
                .role(Role.ROLE_BUYER)
                .deletedAt(deletedAt)
                .build();

        user2 = Users.builder()
                .provider("kakao")
                .providerId("kakao-456")
                .userNameAttribute("response")
                .name("test2")
                .accountDisable(true)
                .role(Role.ROLE_SELLER)
                .deletedAt(deletedAt)
                .build();

        em.persist(user1);
        em.persist(user2);

        buyer = Buyers.builder()
                .user(user1)
                .build();
        em.persist(buyer);

        seller = Sellers.builder()
                .user(user2)
                .build();
        em.persist(seller);
        em.flush();
        em.clear();
    }


    @Test
    @DisplayName("7일 이상 지난 비활성화 유저 조회")
    void selectTargetsTest() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);

        List<Users> targets = mapper.selectTargets(cutoff);

        assertThat(targets).isNotNull();
        assertThat(targets).allSatisfy(u -> {
            assertThat(u.isAccountDisable()).isTrue();
            assertThat(u.getDeletedAt()).isBefore(cutoff);
        });

    }

    @Test
    @DisplayName("BUYER 유저 탈퇴 처리")
    void withdrawBuyerTest() {
        log.info("user1: {}, role: {}", user1.getId(), user1.getRole().toString());
        mapper.withdrawUser(
                user1.getId(),               // userId
                user1.getRole().toString(),
                deletedAt
        );

        WithdrawBuyerDTO user = testMapper.findByBuyersId(user1.getId());
        assertThat(user.role()).isEqualTo("ROLE_WITHDRAWN");
        assertThat(user.accountDisable()).isTrue();
        assertThat(user.deletedAt()).isNotNull();

    }

    @Test
    @DisplayName("SELLER 유저 탈퇴 처리")
    void withdrawSellerTest() {

        mapper.withdrawUser(
                user2.getId(),
                user2.getRole().toString(),
                deletedAt
        );

        Users user = testMapper.findBySellerId(user2.getId());
        assertThat(user.getRole().toString()).isEqualTo("ROLE_WITHDRAWN");
        assertThat(user.isAccountDisable()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
    }
}