package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.mapper.UserMapper;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserTestServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserTestServiceImpl userTestService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Users user;

    @Test
    void test() {
        // Mock the behavior of userMapper.selectOne() to return 2
        when(userMapper.selectOne()).thenReturn(2);

        // Test the service method
        assertThat(userTestService.MapperTest()).isEqualTo(2);
    }

    @Test
    void JpaTest() {
        UUID id = UUID.randomUUID();
        Users user = Users.builder()
                .id(id)
                .name("test")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();
        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(user));

        // 실제 서비스 메서드 호출
        Users result = userTestService.JpaTest(id);

        // 검증
        assertThat(result).isEqualTo(user);
    }
}
