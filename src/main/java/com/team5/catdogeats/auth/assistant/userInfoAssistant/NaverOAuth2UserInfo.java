package com.team5.catdogeats.auth.assistant.userInfoAssistant;

import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class NaverOAuth2UserInfo implements Oauth2UserInfo {
    private final Map<String, Object> attributes;

    @Override
    public String getId() {
        Object res = attributes.get("response");
        if (res instanceof Map<?, ?> map) {
            return (String) map.get("id");
        }
        throw new IllegalStateException("Invalid Naver response structure");
    }


    @Override
    public String getName() {
        Object res = attributes.get("response");
        if (res instanceof Map<?, ?> map) {
            return (String) map.get("name");
        }
        throw new IllegalStateException("Invalid Naver response structure");
    }

}
