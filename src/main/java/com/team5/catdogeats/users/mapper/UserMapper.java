package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT 1+1
    """)
    int selectOne();

    @Update("""
        UPDATE users
        SET account_disable = true
        WHERE provider = #{provider}
        AND provider_id = #{providerId}
    """)
    void softDeleteUserByProviderAndProviderId(String provider, String providerId);
}
