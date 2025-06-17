package com.team5.catdogeats.users.util;

import com.team5.catdogeats.auth.assistant.userInfoAssistant.OAuth2UserInfoFactory;
import com.team5.catdogeats.auth.assistant.userInfoAssistant.Oauth2UserInfo;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuthDTOFactory {
    public OAuthDTO create(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

            Oauth2UserInfo userInfo = OAuth2UserInfoFactory.
                getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        return new OAuthDTO(registrationId, userInfo.getId(), userInfo.getName(), userNameAttributeName);
    }
}
