package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.mapper.UserMapper;
import com.team5.catdogeats.users.service.UserTestMybatisService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@Transactional
@SpringBootTest
class UserTestMybatisServiceImplTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserTestMybatisService userTestService;


    @Test
    void test() {

        // Test the service method
        assertThat(userTestService.MapperTest()).isEqualTo(2);
    }
}