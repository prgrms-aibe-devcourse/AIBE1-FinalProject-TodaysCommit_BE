package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.service.UserDuplicateService;
import com.team5.catdogeats.users.util.OAuthDTOFactory;
import com.team5.catdogeats.users.util.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomOAuth2UserServiceImpl 테스트")
class CustomOAuth2UserServiceImplTest {

    @Mock
    private UserDuplicateService userDuplicateService;

    @Mock
    private OAuthDTOFactory oAuthDTOFactory;

    @Mock
    private UserFactory userFactory;


    @Mock
    private DefaultOAuth2UserService defaultOAuth2UserService;

    @InjectMocks
    private CustomOAuth2UserServiceImpl customService;

    private OAuth2UserRequest userRequest;
    private OAuth2User oAuth2User;
    private OAuthDTO dto;
    private Users users;
    private Map<String, Object> attributes; // attributes 필드 다시 추가

    @BeforeEach
    void setUp() {
        userRequest = mock(OAuth2UserRequest.class);
        oAuth2User = mock(OAuth2User.class);
        dto = mock(OAuthDTO.class);
        users = mock(Users.class);

        attributes = new HashMap<>(); // 초기화

        attributes.put("sub", "provider123");
        attributes.put("name", "구글사용자");

        // 공통 stubbing
        when(oAuth2User.getAttributes()).thenReturn(attributes);

        // 공통 stubbing
        when(defaultOAuth2UserService.loadUser(userRequest)).thenReturn(oAuth2User);
        when(oAuthDTOFactory.create(userRequest, oAuth2User)).thenReturn(dto);
        when(dto.providerId()).thenReturn("provider123");

        // userFactory가 Mock이므로 이 stubbing은 유효합니다.
        when(userFactory.createFromOAuth(dto)).thenReturn(users);
        when(users.getRole()).thenReturn(Role.ROLE_TEMP);

    }

    @Test
    void loadUser_google_success() {
        // given

        when(dto.registrationId()).thenReturn("google");
        when(dto.userNameAttribute()).thenReturn("name");

        // when
        OAuth2User result = customService.loadUser(userRequest);

        // then
        assertThat(result.getAttributes()).isEqualTo(attributes);
        Collection<? extends GrantedAuthority> authorities = result.getAuthorities();
        assertThat(authorities).anyMatch(auth -> auth.getAuthority().equals("ROLE_TEMP"));
        // sub 값이 user1이므로 getName() 결과도 user1이 되어야 함
        assertThat(result.getName()).isEqualTo("구글사용자");
        verify(userDuplicateService).isDuplicate(users);

    }

    @Test
    void loadUser_invalidDto_throwsException() {
        // given
        when(defaultOAuth2UserService.loadUser(userRequest)).thenReturn(oAuth2User);
        when(oAuthDTOFactory.create(userRequest, oAuth2User)).thenReturn(dto);
        when(dto.registrationId()).thenReturn("");
        when(dto.providerId()).thenReturn(null);

        // when/then
        assertThatThrownBy(() -> customService.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }


}