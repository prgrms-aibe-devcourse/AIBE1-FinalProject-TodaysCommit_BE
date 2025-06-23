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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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


    @BeforeEach
    void given_user_in_db() {
        Users user =Users.builder()          // ← @SuperBuilder 붙여둔 상태
                .provider("test")
                .providerId("test")
                .userNameAttribute("test")
                .name("tester")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();
        userRepository.save(user);

    }

    @Test
    void softDeleteUserByProviderAndProviderId() {
        // when
        em.flush();
        em.clear();

        int rows = userMapper.softDeleteUserByProviderAndProviderId(
                "test", "test", OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(rows).isEqualTo(1);


        // then
        Users updated = userRepository.findByProviderAndProviderId("test","test")
                .orElseThrow();
        log.info("updated: {}", updated.getRole());
        assertThat(updated.isAccountDisable()).isTrue();
    }
}
