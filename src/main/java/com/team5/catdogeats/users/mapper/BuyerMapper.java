package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface BuyerMapper {

    @Update("""
    
      UPDATE buyers
      SET is_deleted = true , deleted_at = #{now}
      WHERE user_id = (
        SELECT id FROM users WHERE provider = #{provider} AND provider_id = #{providerId}
      )
    """)
    void softDeleteBuyerByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId, @Param("now") OffsetDateTime now);
}
