package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.mapper.BuyerMapper;
import com.team5.catdogeats.users.mapper.SellerMapper;
import com.team5.catdogeats.users.mapper.UserMapper;
import com.team5.catdogeats.users.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {
    private final UserMapper userMapper;
    private final BuyerMapper buyerMapper;
    private final SellerMapper sellerMapper;

    @Override
    @Transactional
    public void withdraw(UserPrincipal userPrincipal, Role role) {
        if (role == Role.ROLE_TEMP) {
            throw new IllegalStateException("잘못된 권한을 가진 유저입니다.");
        }

        if (role == Role.ROLE_SELLER) {
            userMapper.softDeleteUserByProviderAndProviderId(userPrincipal.provider(),
                    userPrincipal.providerId());
            sellerMapper.softDeleteSellerByProviderAndProviderId(userPrincipal.provider(),
                    userPrincipal.providerId(), ZonedDateTime.now());
        }

        if (role == Role.ROLE_BUYER) {
            userMapper.softDeleteUserByProviderAndProviderId(userPrincipal.provider(),
                    userPrincipal.providerId());
            buyerMapper.softDeleteBuyerByProviderAndProviderId(userPrincipal.provider(),
                    userPrincipal.providerId(), ZonedDateTime.now());
        }
    }
}
