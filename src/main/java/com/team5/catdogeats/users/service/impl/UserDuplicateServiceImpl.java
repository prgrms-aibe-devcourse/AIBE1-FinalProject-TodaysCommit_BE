package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.UserDuplicateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDuplicateServiceImpl implements UserDuplicateService {
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;

    @Transactional
    public Users isDuplicate(Users users) {
        try {
            log.info("isDuplicate 확인하기 {}", users);
            Optional<Users> user = userRepository.findByProviderAndProviderId(
                    users.getProvider(),
                    users.getProviderId());
            if(user.isPresent()) {
                Users userOpt = user.get();
                userOpt.preUpdate();
                log.info("{}", userOpt);
                return userRepository.save(userOpt);

            }
            return userRepository.save(users);
        } catch (Exception e) {
            log.error("유저 정보 저장 중 오류 발생");
            throw new RuntimeException(e);
        }
    }
}
