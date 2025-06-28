package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, String> {
    Optional<Users> findByProviderAndProviderId(String provider, String providerId);

    Optional<Users> findById(String id);
    boolean existsById(String id);


    Users getReferenceById(String id);

    @Query("""
            SELECT u.name FROM Users u WHERE u.id = :id
        """)
    Optional<String> findNameById(@Param("id") String id);}
