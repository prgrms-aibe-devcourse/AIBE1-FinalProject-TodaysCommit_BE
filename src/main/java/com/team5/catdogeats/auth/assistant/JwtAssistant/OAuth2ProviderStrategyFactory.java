package com.team5.catdogeats.auth.assistant.JwtAssistant;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class OAuth2ProviderStrategyFactory {
    private final Map<String, OAuth2ProviderStrategy> strategies;

    public OAuth2ProviderStrategyFactory(List<OAuth2ProviderStrategy> strategyList) {
        this.strategies = Map.of("google", findStrategy(strategyList,GoogleOAuth2Strategy.class),
                "kakao", findStrategy(strategyList,KakaoOAuth2Strategy.class),
                "naver", findStrategy(strategyList,NaverOAuth2Strategy.class));

    }

    public OAuth2ProviderStrategy getStrategy(String provider) {
        OAuth2ProviderStrategy strategy = strategies.get(provider);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for provider: " + provider);
        }
        return strategy;
    }

    private OAuth2ProviderStrategy findStrategy(List<OAuth2ProviderStrategy> strategies,
                                                Class<? extends OAuth2ProviderStrategy> strategyClass) {
        return strategies.stream()
                .filter(strategyClass::isInstance)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No strategy found for " + strategyClass.getSimpleName()));
    }
}
