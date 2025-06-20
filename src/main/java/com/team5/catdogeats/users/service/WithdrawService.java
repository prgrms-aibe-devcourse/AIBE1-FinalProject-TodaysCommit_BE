package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;

public interface WithdrawService {
    void withdraw(UserPrincipal userPrincipal);
}
