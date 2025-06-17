package com.team5.catdogeats.users.service;

import com.team5.catdogeats.users.domain.Users;

public interface UserDuplicateService {
    Users isDuplicate(Users users);
}
