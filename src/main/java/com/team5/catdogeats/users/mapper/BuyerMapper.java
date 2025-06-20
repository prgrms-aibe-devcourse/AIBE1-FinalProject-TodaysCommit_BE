package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Update;

import java.time.ZonedDateTime;

public interface BuyerMapper {

    @Update("""
    
      UPDATE buyers
      SET is_deleted = true, deleted_at = #{now}
      WHERE user_id = (
        SELECT id FROM users WHERE provider = #{provider} AND provider_id = #{providerId}
      )
    """)
    void softDeleteBuyerByProviderAndProviderId(String provider, String providerId, ZonedDateTime now);
}
