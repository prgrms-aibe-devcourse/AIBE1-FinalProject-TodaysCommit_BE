package com.team5.catdogeats.users.domain.dto;

public record OAuthDTO(String registrationId,
                       String providerId,
                       String name,
                       String userNameAttribute) {
}
