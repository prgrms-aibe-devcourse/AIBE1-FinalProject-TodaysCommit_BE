package com.team5.catdogeats.chats.domain.dto;

import com.team5.catdogeats.auth.dto.UserPrincipal;

public record InterceptorDTO(String provider,
                             String providerId,
                             String role,
                             UserPrincipal userPrincipal) {
}
