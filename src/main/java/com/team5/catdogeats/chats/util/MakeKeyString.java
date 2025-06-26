package com.team5.catdogeats.chats.util;


public class MakeKeyString {
    public static String makeKeyProviderAndProviderId(String provider, String providerId) {
        return provider + ":" + providerId;
    }

    public static String makeRoomId(String key, String value) {
        return key + ":" + value;
    }
}
