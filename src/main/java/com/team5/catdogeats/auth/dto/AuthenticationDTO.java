package com.team5.catdogeats.auth.dto;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public record AuthenticationDTO(List<SimpleGrantedAuthority> authorities,
                                OAuth2ProviderStrategy providerStrategy) {
}
