package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.UserTestJpaService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Transactional
@SpringBootTest
class UserTestJpaServiceImplTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTestJpaService userTestJpaService;

    @Test
    void JpaTest() {
        Users user = Users.builder()
                .name("test")
                .role(Role.ROLE_BUYER)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();

        userRepository.save(user);

        Users result = userTestJpaService.JpaTest(user.getId());

        assertThat(result.getId()).isEqualTo(user.getId());
    }

}