package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.OffsetDateTime;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT 1+1
    """)
    int selectOne();

    @Update("""
    UPDATE users
       SET account_disable = true , deleted_at = #{now}
       WHERE  provider = #{provider}
       AND provider_id = #{providerId}
""")
    int softDeleteUserByProviderAndProviderId(
            @Param("provider")   String provider,
            @Param("providerId") String providerId,
            @Param("now") OffsetDateTime now);

}
