package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategy;
import com.team5.catdogeats.auth.assistant.JwtAssistant.OAuth2ProviderStrategyFactory;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.ModifyRoleRequestDTO;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.ModifyUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ModifyUserRoleServiceImpl  implements ModifyUserRoleService {
    private final UserRepository userRepository;
    private final SellersRepository sellersRepository;
    private final BuyerRepository buyerRepository;
    private final OAuth2ProviderStrategyFactory strategyFactory;

    @Override
    @Transactional
    public Authentication modifyUserRole(UserPrincipal userPrincipal, ModifyRoleRequestDTO role) {
        Users user = userRepository.findByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        if (user.getRole() != Role.ROLE_TEMP) {
            throw new IllegalStateException("권한을 변경하실 수 없습니다.");
        }

        user.updateRole(role.role());
        userRepository.save(user);


        if (role.role() == Role.ROLE_SELLER) {
            Sellers seller = Sellers.builder()
                    .user(user)
                    .build();
            sellersRepository.save(seller);
        }

        if (role.role() == Role.ROLE_BUYER) {
            Buyers buyer = Buyers.builder()
                    .user(user)
                    .build();
            buyerRepository.save(buyer);
        }

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuth = modifyAuthentication(currentAuth, userPrincipal, role, user);
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        return newAuth;
    }

    private Authentication modifyAuthentication(
            Authentication authentication,
            UserPrincipal userPrincipal,
            ModifyRoleRequestDTO role,
            Users user) {
        if (authentication == null) {
            throw new IllegalArgumentException("인증 정보가 존재하지 않습니다.");
        }

        // 새로운 권한 부여
        Collection<GrantedAuthority> updatedAuthorities = List.of(
                new SimpleGrantedAuthority(role.toString())
        );

        // 사용자 속성 구성
        OAuth2ProviderStrategy strategy = strategyFactory.getStrategy(userPrincipal.provider());
        Map<String, Object> attributes = strategy.buildUserAttributes(user);

        OAuth2User oAuth2User = new DefaultOAuth2User(
                updatedAuthorities,
                attributes,
                user.getUserNameAttribute()
        );

        return new OAuth2AuthenticationToken(
                oAuth2User,
                updatedAuthorities,
                userPrincipal.provider()
        );
    }

}
