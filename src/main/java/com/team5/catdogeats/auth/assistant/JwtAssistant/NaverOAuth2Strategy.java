package com.team5.catdogeats.auth.assistant.JwtAssistant;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NaverOAuth2Strategy implements OAuth2ProviderStrategy {
    @Override
    public String extractProviderId(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        return response != null ? (String) response.get("id") : null;
    }

    @Override
    public Map<String, Object> buildUserAttributes(Users user) {
        Map<String, Object> response = Map.of(
                "id", user.getProviderId(),
                "name", user.getName());

        return Map.of("response", response);
    }
}
