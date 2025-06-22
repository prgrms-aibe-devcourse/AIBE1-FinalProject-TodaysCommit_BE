package com.team5.catdogeats.users.mapper;

import com.team5.catdogeats.users.domain.Users;
import org.apache.ibatis.annotations.*;

import java.time.OffsetDateTime;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT 1+1
    """)
    int selectOne();

    @Update("""
    UPDATE users
       SET account_disable = true , deled_at = #{now}
       WHERE  provider = #{provider}
       AND provider_id = #{providerId}
""")
    int softDeleteUserByProviderAndProviderId(
            @Param("provider")   String provider,
            @Param("providerId") String providerId,
            @Param("now") OffsetDateTime now);


    @Insert("""
  INSERT INTO users
       (id, provider, provider_id, user_name_attribute,
        name, role, account_disable, created_at, updated_at)
      VALUES
       (#{id}, #{provider}, #{providerId}, #{userNameAttribute},
        #{name}, #{role}, #{accountDisable},
        #{createdAt}, #{updatedAt})
""")
    int insert(Users user);
}
