package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.assistant.userInfoAssistant.OAuth2UserInfoFactory;
import com.team5.catdogeats.auth.assistant.userInfoAssistant.Oauth2UserInfo;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.service.UserDuplicateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserDuplicateService userDuplicateService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = getOAuth2User(userRequest);
            OAuthDTO dto = buildDTO(userRequest, oAuth2User);
            validateOAuthInfo(dto);
            Users savedUsers = saveOrUpdateUser(dto);
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority(savedUsers.getRole().toString())),
                    oAuth2User.getAttributes(),
                    dto.userNameAttribute()
            );
        } catch (OAuth2AuthenticationException e) {
            log.error("OAuth2User loadUser error", e);
            throw e;
        }
    }

    private void validateOAuthInfo(OAuthDTO dto) {
        if (dto.registrationId().isEmpty() || dto.providerId() == null) {
            log.error("providerId or registrationId is null or empty");
            throw new OAuth2AuthenticationException(new OAuth2Error("OAuth provider 의 정보가 없습니다."));
        }
    }

    OAuth2User getOAuth2User(OAuth2UserRequest userRequest) {
        OAuth2UserService<OAuth2UserRequest, OAuth2User>  delegate = new DefaultOAuth2UserService();
        return delegate.loadUser(userRequest);
    }

    private OAuthDTO buildDTO(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttribute = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        Oauth2UserInfo userInfo = OAuth2UserInfoFactory
                .getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        String providerId = userInfo.getId();
        String name = userInfo.getName();
        return new OAuthDTO(registrationId, providerId, name, userNameAttribute);
    }

    private Users saveOrUpdateUser(OAuthDTO dto) {
        Users users = Users.builder()
                .role(Role.ROLE_TEMP)
                .provider(dto.registrationId())
                .providerId(dto.providerId())
                .name(dto.name())
                .userNameAttribute(dto.userNameAttribute())
                .build();

        return userDuplicateService.isDuplicate(users);
    }
}
