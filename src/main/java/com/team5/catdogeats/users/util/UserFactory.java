package com.team5.catdogeats.users.util;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.OAuthDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    public Users createFromOAuth(OAuthDTO dto) {
        return Users.builder()
                .role(Role.ROLE_TEMP)
                .provider(dto.registrationId())
                .providerId(dto.providerId())
                .name(dto.name())
                .userNameAttribute(dto.userNameAttribute())
                .build();
    }
}
