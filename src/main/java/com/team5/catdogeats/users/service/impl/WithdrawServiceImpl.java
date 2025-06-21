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

        log.info("회원 탈퇴 시작");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String firstAuthority = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElseThrow(() -> new IllegalStateException("권한 정보가 없습니다."));

            if (Objects.equals(firstAuthority, Role.ROLE_TEMP.toString())) {
                throw new IllegalStateException("잘못된 권한을 가진 유저입니다.");
            }
            log.debug("firstAuthority: {}", firstAuthority);
            userMapper.softDeleteUserByProviderAndProviderId(userPrincipal.provider(),
                    userPrincipal.providerId(), Role.ROLE_WITHDRAWN.toString());

            if (Objects.equals(firstAuthority, Role.ROLE_SELLER.toString())) {
                log.info("seller 탈퇴 시작");
                sellerMapper.softDeleteSellerByProviderAndProviderId(userPrincipal.provider(),
                        userPrincipal.providerId(), OffsetDateTime.now(ZoneOffset.UTC));
            }

            if (Objects.equals(firstAuthority, Role.ROLE_BUYER.toString())) {
                log.info("buyer 탈퇴 시작");
                buyerMapper.softDeleteBuyerByProviderAndProviderId(userPrincipal.provider(),
                        userPrincipal.providerId(), OffsetDateTime.now(ZoneOffset.UTC));
            }
        } catch (BadSqlGrammarException e) {
            log.error("sql 에러", e);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("무슨에러이?");
        }
    }
}
