package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BuyerRepository extends JpaRepository<Buyers, UUID> {

}
