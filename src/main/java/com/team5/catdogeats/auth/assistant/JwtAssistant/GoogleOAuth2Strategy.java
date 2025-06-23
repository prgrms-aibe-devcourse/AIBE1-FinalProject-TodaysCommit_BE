package com.team5.catdogeats.auth.assistant.JwtAssistant;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GoogleOAuth2Strategy implements OAuth2ProviderStrategy {
    @Override
    public String extractProviderId(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("sub");
    }

    @Override
    public Map<String, Object> buildUserAttributes(Users user) {
        return Map.of("sub", user.getProviderId(),
                "name",user.getName());
    }
}
