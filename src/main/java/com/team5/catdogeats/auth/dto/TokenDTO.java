package com.team5.catdogeats.auth.dto;

import java.time.ZonedDateTime;

public record TokenDTO(String providerId,
                       String authorities,
                       String registrationId,
                       ZonedDateTime now,
                       ZonedDateTime expiration) {
}
