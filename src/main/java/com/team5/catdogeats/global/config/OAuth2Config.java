package com.team5.catdogeats.global.config;

import com.team5.catdogeats.users.service.UserDuplicateService;
import com.team5.catdogeats.users.service.impl.CustomOAuth2UserServiceImpl;
import com.team5.catdogeats.users.util.OAuthDTOFactory;
import com.team5.catdogeats.users.util.UserFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Configuration
public class OAuth2Config {
    @Bean
    public DefaultOAuth2UserService defaultOAuth2UserService() {
        return new DefaultOAuth2UserService();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService(
            DefaultOAuth2UserService defaultOAuth2UserService,
            UserDuplicateService userDuplicateService,
            OAuthDTOFactory oAuthDTOFactory,
            UserFactory userFactory) {
        return new CustomOAuth2UserServiceImpl(
                userDuplicateService,
                oAuthDTOFactory,
                userFactory,
                defaultOAuth2UserService
        );
    }
}

