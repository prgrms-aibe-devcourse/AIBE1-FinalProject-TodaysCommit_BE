package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class RefreshTokenServiceImplTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("리프레시 토큰 생성")
    void createRefreshToken() {


        refreshTokenService.createRefreshToken(null);
    }

}