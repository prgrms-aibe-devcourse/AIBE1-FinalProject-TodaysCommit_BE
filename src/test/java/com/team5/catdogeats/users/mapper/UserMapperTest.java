package com.team5.catdogeats.users.mapper;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
@Slf4j
class UserMapperTest {

    @Autowired UserMapper userMapper;
    @Autowired
    UserRepository userRepository;
    @Autowired
    BuyerRepository buyerRepository;
    @Autowired
    private EntityManager em;

    // ────────────────── 테스트용 이너 static ──────────────────
    private static class Fixture {
        /** 기본 Users 한 명 만들어 주는 팩토리 */
        static Users newUser(Role role) {
            return Users.builder()          // ← @SuperBuilder 붙여둔 상태
                    .provider("test")
                    .providerId("test")
                    .userNameAttribute("test")
                    .name("tester")
                    .role(role)
                    .accountDisable(false)
                    // createdAt / updatedAt 은 JPA @PrePersist 가 채움
                    .build();
        }
    }
    // ─────────────────────────────────────────────────────────

    @BeforeEach
    void given_user_in_db() {
        Users users = userRepository.save(Fixture.newUser(Role.ROLE_BUYER));
//        Buyers buyers= Buyers.builder()
//                .userId(users.getId()).build();
//        newBuyers = buyerRepository.save(buyers);
    }

    @Test
    void softDeleteUserByProviderAndProviderId() {
        // when
        em.flush();
        em.clear();   // ← 1차 캐시 비움

        int rows = userMapper.softDeleteUserByProviderAndProviderId(
                "test", "test", Role.ROLE_WITHDRAWN.toString());
        assertThat(rows).isEqualTo(1);     // 0 이면 WHERE 조건이 안 맞은 것




        // then
        Users updated = userRepository.findByProviderAndProviderId("test","test")
                .orElseThrow();
        log.info("updated: {}", updated.getRole());
        assertThat(updated.isAccountDisable()).isTrue();
        assertThat(updated.getRole()).isEqualTo(Role.ROLE_WITHDRAWN);
    }
}
