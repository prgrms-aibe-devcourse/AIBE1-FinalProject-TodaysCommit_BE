package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.UserTestJpaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserTestJpaServiceImpl implements UserTestJpaService {
    private final UserRepository userRepository;

    @Override
    public Users JpaTest(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("유저 없음"));
    }
}
