package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ModifyRoleRequestDTO(@NotNull Role role) {
}
