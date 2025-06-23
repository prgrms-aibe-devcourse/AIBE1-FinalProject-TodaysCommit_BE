package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.global.exception.WithdrawnAccountException;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.exception.WithdrawnAccountDomainException;
import com.team5.catdogeats.users.service.UserDuplicateService;
import com.team5.catdogeats.users.util.OAuthDTOFactory;
import com.team5.catdogeats.users.util.UserFactory;
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
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserDuplicateService userDuplicateService;
    private final OAuthDTOFactory oAuthDTOFactory;
    private final UserFactory userFactory;
    private final DefaultOAuth2UserService defaultOAuth2UserService;
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        try {
            OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(userRequest);
            OAuthDTO dto = oAuthDTOFactory.create(userRequest, oAuth2User);
            validateOAuthInfo(dto);

            Users savedUsers = userFactory.createFromOAuth(dto);
            Users users = userDuplicateService.isDuplicate(savedUsers);
            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority(savedUsers.getRole().toString())),
                    oAuth2User.getAttributes(),
                    dto.userNameAttribute()
            );

        } catch (WithdrawnAccountDomainException e) {   // 도메인 예외 -> runtime error
            throw new WithdrawnAccountException(); // 스프링 시큐리티 핸들러에 잡히는 예외
        }  catch (OAuth2AuthenticationException e) {
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
}
