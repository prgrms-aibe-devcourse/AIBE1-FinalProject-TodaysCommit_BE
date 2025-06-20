package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.assistant.userInfoAssistant.OAuth2UserInfoFactory;
import com.team5.catdogeats.auth.assistant.userInfoAssistant.Oauth2UserInfo;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.service.UserDuplicateService;
import com.team5.catdogeats.users.util.OAuthDTOFactory;
import com.team5.catdogeats.users.util.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

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

    // 모든 중첩 테스트에서 공유될 수 있는 Mock 데이터
    protected OAuth2UserRequest userRequest;
    protected OAuth2User oAuth2User;
    protected OAuthDTO dto;
    protected Users users;

    @BeforeEach // 모든 테스트가 실행되기 전에 공통적으로 실행되는 설정
    void commonSetup() {
        userRequest = mock(OAuth2UserRequest.class);
        oAuth2User = mock(OAuth2User.class);
        dto = mock(OAuthDTO.class);
        users = mock(Users.class);

        // 공통 stubbing (대부분의 제공자에서 동일하게 적용되는 부분)
        when(defaultOAuth2UserService.loadUser(userRequest)).thenReturn(oAuth2User);
        when(oAuthDTOFactory.create(userRequest, oAuth2User)).thenReturn(dto);
        when(userFactory.createFromOAuth(dto)).thenReturn(users);
        when(users.getRole()).thenReturn(Role.ROLE_TEMP); // 예시 역할
    }

    @Nested
    @DisplayName("Google Provider 테스트")
    class GoogleTests {
        private Map<String, Object> googleAttrs;

        @BeforeEach // Google 테스트를 위한 개별 설정
        void googleSetup() {
            googleAttrs = new HashMap<>();
            googleAttrs.put("sub", "google_provider_id_123");
            googleAttrs.put("name", "구글사용자");
            when(oAuth2User.getAttributes()).thenReturn(googleAttrs);

            when(dto.registrationId()).thenReturn("google");
            when(dto.providerId()).thenReturn("google_provider_id_123");
            when(dto.userNameAttribute()).thenReturn("name"); // Google의 이름 속성은 'name'
            when(dto.name()).thenReturn("구글사용자"); // OAuthDTOFactory가 userInfo.getName()을 호출하므로 DTO에도 설정
            when(dto.providerId()).thenReturn("google_provider_id_123"); // OAuthDTOFactory가 userInfo.getId()를 호출하므로 DTO에도 설정
        }

        @Test
        @DisplayName("성공적으로 Google OAuth 유저 로드")
        void loadUser_google_success() {
            OAuth2User result = customService.loadUser(userRequest);

            assertThat(result.getAttributes()).isEqualTo(googleAttrs);
            assertThat(result.getName()).isEqualTo("구글사용자");
            assertThat(result.getAuthorities())
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEMP"));

            verify(userDuplicateService).isDuplicate(users);
        }

        @Test
        @DisplayName("Google OAuth 정보 유효성 검증 실패 (providerId 없음)")
        void loadUser_google_invalidProviderId_throwsException() {
            when(dto.providerId()).thenReturn(null); // 특정 테스트를 위해 오버라이드
            assertThatThrownBy(() -> customService.loadUser(userRequest))
                    .isInstanceOf(OAuth2AuthenticationException.class);
        }
    }

    @Nested
    @DisplayName("Naver Provider 테스트")
    class NaverTests {
        private Map<String, Object> naverAttrs;

        @BeforeEach // Naver 테스트를 위한 개별 설정
        void naverSetup() {
            // Naver의 경우, 속성이 'response' 하위에 중첩되는 경우가 많음
            naverAttrs = new HashMap<>();
            naverAttrs.put("response", Map.of(
                    "id", "naver_provider_id_456",
                    "name", "네이버사용자"
            ));
            when(oAuth2User.getAttributes()).thenReturn(naverAttrs);

            when(dto.registrationId()).thenReturn("naver");
            when(dto.providerId()).thenReturn("naver_provider_id_456");
            when(dto.userNameAttribute()).thenReturn("response");
            when(dto.name()).thenReturn("네이버사용자");
            when(dto.providerId()).thenReturn("naver_provider_id_456");
        }

        @Test
        @DisplayName("성공적으로 Naver OAuth 유저 로드")
        void loadUser_naver_success() {
            OAuth2User result = customService.loadUser(userRequest);
            assertThat(result.getAttributes()).isEqualTo(naverAttrs);
            Oauth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(dto.registrationId(), result.getAttributes());
            assertThat(userInfo.getName()).isEqualTo("네이버사용자");
            assertThat(result.getAuthorities())
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEMP"));

            verify(userDuplicateService).isDuplicate(users);
        }

    }

    @Nested
    @DisplayName("Kakao Provider 테스트")
    class KakaoTests {
        private Map<String, Object> kakaoAttrs;

        @BeforeEach // Kakao 테스트를 위한 개별 설정
        void kakaoSetup() {
            // Kakao의 경우, 속성이 'kakao_account' 하위에 중첩되고, 'profile' 하위에 'nickname'이 있음
            kakaoAttrs = new HashMap<>();
            kakaoAttrs.put("id", "kakao_provider_id_789"); // Kakao의 providerId는 최상위 id
            kakaoAttrs.put("properties", Map.of(
                    "nickname", "카카오사용자"
            ));

            when(oAuth2User.getAttributes()).thenReturn(kakaoAttrs);

            when(dto.registrationId()).thenReturn("kakao");
            when(dto.providerId()).thenReturn("kakao_provider_id_789");
            when(dto.userNameAttribute()).thenReturn("id"); // 실제 Kakao에서 이름 속성이 어떻게 추출되는지 확인 필요
            when(dto.name()).thenReturn("카카오사용자");
            when(dto.providerId()).thenReturn("kakao_provider_id_789");
        }

        @Test
        @DisplayName("성공적으로 Kakao OAuth 유저 로드")
        void loadUser_kakao_success() {
            OAuth2User result = customService.loadUser(userRequest);

            assertThat(result.getAttributes()).isEqualTo(kakaoAttrs);
            Oauth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(dto.registrationId(), result.getAttributes());
            assertThat(userInfo.getName()).isEqualTo("카카오사용자");

            assertThat(result.getAuthorities())
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEMP"));

            verify(userDuplicateService).isDuplicate(users);
        }

        // Kakao 관련 실패 테스트들도 여기에 추가
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