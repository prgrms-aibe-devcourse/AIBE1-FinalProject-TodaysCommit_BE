package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.ModifyRoleRequestDTO;
import org.springframework.security.core.Authentication;

public interface ModifyUserRoleService {
    Authentication modifyUserRole(UserPrincipal userPrincipal, ModifyRoleRequestDTO role);
}
