package com.team5.catdogeats.global.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class CookieProperties {

    @Value( "${jwt.cookie.secure}" )
    boolean secure;
    @Value( "${jwt.cookie.same-site}" )
    String sameSite;
    @Value( "${jwt.cookie.http-only}" )
    boolean httpOnly;
    @Value( "${jwt.cookie.max-age}" )
    long maxAge;
    @Value( "${jwt.cookie.domain}" )
    String domain;
    @Value( "${jwt.cookie.path}" )
    String path;
}
