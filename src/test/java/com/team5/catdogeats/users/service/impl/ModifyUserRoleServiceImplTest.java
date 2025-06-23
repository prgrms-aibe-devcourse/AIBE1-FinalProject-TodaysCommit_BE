package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.ModifyRoleRequestDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
@Slf4j
class ModifyUserRoleServiceImplTest {

    @Autowired
    private ModifyUserRoleServiceImpl modifyUserRoleService;

    @Autowired
    private UserRepository userRepository;

    private Users user;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        // --- Users 엔티티의 모든 NOT NULL 컬럼 채우기 ---
        user = Users.builder()
                .provider("google")
                .providerId("12345")
                .userNameAttribute("sub")          // 필수
                .name("테스트 유저")                 // 필수
                .role(Role.ROLE_TEMP)
                .build();
        userRepository.save(user);

        userPrincipal = new UserPrincipal("google", "12345");

        // SecurityContextHolder에 기존 인증 세팅
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(Role.ROLE_TEMP.toString())
        );        Map<String, Object> attrs = Map.of("sub", user.getProviderId());
        DefaultOAuth2User oldUser = new DefaultOAuth2User(authorities, attrs, "sub");
        Authentication oldAuth = new OAuth2AuthenticationToken(oldUser, authorities, "google");
        SecurityContextHolder.getContext().setAuthentication(oldAuth);
    }

    @Test
    void modifyUserRole_ShouldThrow_WhenUserNotFound() {
        UserPrincipal wrongPrincipal = new UserPrincipal("google", "wrong-id");
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () ->
                modifyUserRoleService.modifyUserRole(wrongPrincipal, new ModifyRoleRequestDTO(Role.ROLE_BUYER)));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void modifyUserRole_ShouldThrow_WhenRoleIsNotTemp() {
        // 이미 ROLE_TEMP가 아니면 IllegalState
        user.updateRole(Role.ROLE_BUYER);
        userRepository.save(user);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                modifyUserRoleService.modifyUserRole(userPrincipal, new ModifyRoleRequestDTO(Role.ROLE_BUYER)));
        assertEquals("권한을 변경하실 수 없습니다.", ex.getMessage());
    }

    @Test
    void modifyUserRole_ShouldUpdateRoleAndAuthentication() {
        // 요청 DTO
        ModifyRoleRequestDTO dto = new ModifyRoleRequestDTO(Role.ROLE_BUYER);

        Authentication newAuth = modifyUserRoleService.modifyUserRole(userPrincipal, dto);

        // 1) DB에 반영 확인
        Users updated = userRepository.findByProviderAndProviderId("google", "12345").orElseThrow();
        assertEquals(Role.ROLE_BUYER, updated.getRole());

        // 2) 반환된 Authentication 확인
        assertNotNull(newAuth);
        assertTrue(newAuth.isAuthenticated());
        assertEquals("google", ((OAuth2AuthenticationToken)newAuth).getAuthorizedClientRegistrationId());
        String expectedAuthority = dto.role().toString();
        log.info("expectedAuthority: {}", expectedAuthority);
        assertTrue(newAuth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals(expectedAuthority)),
                () -> "Expected authority " + expectedAuthority + " but was " +
                        newAuth.getAuthorities());
    }
}
