package com.team5.catdogeats.auth.assistant.JwtAssistant;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public interface OAuth2ProviderStrategy {
    String extractProviderId(OAuth2User oAuth2User);
    Map<String, Object> buildUserAttributes(Users users);
}
