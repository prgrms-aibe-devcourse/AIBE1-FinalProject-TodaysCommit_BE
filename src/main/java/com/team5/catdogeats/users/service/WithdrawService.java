package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.enums.Role;

public interface WithdrawService {
    void withdraw(UserPrincipal userPrincipal, Role role);
}
