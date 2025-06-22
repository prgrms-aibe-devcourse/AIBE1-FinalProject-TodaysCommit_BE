package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.exception.WithdrawnAccountDomainException;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class UserDuplicateServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomOAuth2UserServiceImpl customOAuth2UserService;

    @InjectMocks
    private UserDuplicateServiceImpl userDuplicateService;


    private final String provider = "google";
    private final String providerId = "12345";

    protected OAuth2UserRequest userRequest;
    protected OAuth2User oAuth2User;
    protected OAuthDTO dto;
    protected Users users;

    @Test
    void test_신규유저_저장() {
        // given
        Users newUser = Users.builder()
                .provider(provider)
                .providerId(providerId)
                .build();

        when(userRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.empty());
        when(userRepository.save(newUser)).thenReturn(newUser);

        // when
        Users result = userDuplicateService.isDuplicate(newUser);

        // then
        assertEquals(newUser, result);
        verify(userRepository).save(newUser);
    }

        @Test
        void test_기존유저_비활성화_7일이내_재활성화() {
            // given
            OffsetDateTime dateTime = OffsetDateTime.now().minusDays(3);
            Users user = Users.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .role(Role.ROLE_BUYER)
                    .accountDisable(true)
                    .deletedAt(dateTime)
                    .build();
            when(userRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(Users.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Users result = userDuplicateService.isDuplicate(user);

            assertFalse(result.isAccountDisable());   // 재활성화 확인
            assertNull(result.getDeletedAt());        // deletedAt 초기화 확인 (정책에 따라)
            verify(userRepository).save(user);
            verifyNoMoreInteractions(userRepository);
        }


    @Test
    void test_비상활성화_7일이후_계정() {
        userRequest = mock(OAuth2UserRequest.class);
        oAuth2User = mock(OAuth2User.class);
        dto = mock(OAuthDTO.class);
        users = mock(Users.class);

        OffsetDateTime dateTime = OffsetDateTime.now().minusDays(9);
        Users user = Users.builder()
                .provider(provider)
                .providerId(providerId)
                .role(Role.ROLE_WITHDRAWN)
                .accountDisable(true)
                .deletedAt(dateTime)
                .build();
        when(userRepository.findByProviderAndProviderId(provider, providerId)).thenReturn(Optional.of(user));

        assertThrows(
                WithdrawnAccountDomainException.class,   // ← 서비스 전용 예외
                () -> userDuplicateService.isDuplicate(user)
        );
        verify(userRepository, never()).save(any());   // 저장이 호출되지 않는지 추가 검증
    }

}
