package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<Users, UUID> {

}
