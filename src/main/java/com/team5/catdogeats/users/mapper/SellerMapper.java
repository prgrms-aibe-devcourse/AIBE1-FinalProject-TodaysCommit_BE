package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.ZonedDateTime;

@Mapper
public interface SellerMapper {
    @Update("""
      UPDATE sellers s
      SET s.is_deleted = true, s.deleted_at = #{now}
      WHERE s.user_id = (
        SELECT u.id FROM users u WHERE u.provider = #{provider} AND u.provider_id = #{providerId}
      )
    """)
    void softDeleteSellerByProviderAndProviderId(String provider, String providerId, ZonedDateTime now);
}
