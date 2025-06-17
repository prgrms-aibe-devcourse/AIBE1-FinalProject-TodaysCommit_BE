package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.mapper.UserMapper;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.UserTestMybatisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserTestMybatisServiceImpl implements UserTestMybatisService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    public int MapperTest() {
        return userMapper.selectOne();
    }



}
