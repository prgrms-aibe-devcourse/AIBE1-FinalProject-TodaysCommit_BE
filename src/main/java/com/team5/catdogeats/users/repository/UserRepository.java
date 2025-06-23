package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<Users, UUID> {
    Optional<Users> findByProviderAndProviderId(String provider, String providerId);

    Optional<Users> findById(String id);
    boolean existsById(String id);


    Users getReferenceById(String id);
}
