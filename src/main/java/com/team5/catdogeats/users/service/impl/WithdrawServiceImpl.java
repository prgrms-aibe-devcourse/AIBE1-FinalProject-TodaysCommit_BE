package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.mapper.BuyerMapper;
import com.team5.catdogeats.users.mapper.SellerMapper;
import com.team5.catdogeats.users.mapper.UserMapper;
import com.team5.catdogeats.users.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawServiceImpl implements WithdrawService {
    private final UserMapper userMapper;
    private final BuyerMapper buyerMapper;
    private final SellerMapper sellerMapper;

    @Override
    @Transactional
    public void withdraw(UserPrincipal userPrincipal) {

        log.debug("회원 탈퇴 시작");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String firstAuthority = validate(authentication);
            log.debug("firstAuthority: {}", firstAuthority);

            withdrawWithRoleTrigger(userPrincipal, firstAuthority);

        } catch (BadSqlGrammarException e) {
            log.error("sql 에러", e);
            throw e;
        }
    }

    private void withdrawWithRoleTrigger(UserPrincipal userPrincipal, String firstAuthority) {
        userMapper.softDeleteUserByProviderAndProviderId(userPrincipal.provider(),
                                                        userPrincipal.providerId(),
                                                        OffsetDateTime.now(ZoneOffset.UTC));
        switch (firstAuthority) {
            case "ROLE_BUYER" -> buyerMapper.softDeleteBuyerByProviderAndProviderId(userPrincipal.provider(),
                                                userPrincipal.providerId(), OffsetDateTime.now(ZoneOffset.UTC));

            case "ROLE_SELLER" -> sellerMapper.softDeleteSellerByProviderAndProviderId(userPrincipal.provider(),
                                                userPrincipal.providerId(), OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

    private String validate(Authentication authentication) {
        String firstAuthority = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new IllegalStateException("권한 정보가 없습니다."));

        if (Objects.equals(firstAuthority, Role.ROLE_TEMP.toString()) || Objects.equals(firstAuthority, Role.ROLE_WITHDRAWN.toString())) {
            throw new IllegalStateException("잘못된 권한을 가진 유저입니다.");
        }
        return firstAuthority;
    }
}
