package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.users.domain.Users;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper
public interface UserWithdrawMapper {

    @Select("""
       SELECT id, role, account_disable, deleted_at
         FROM users
        WHERE account_disable = true
          AND deleted_at     <= #{cutoff}
       """)
    List<Users> selectTargets(@Param("cutoff") OffsetDateTime cutoff);

    @Lang(XMLLanguageDriver.class)
    @Update("""
        <script>
        UPDATE users
           SET role  = 'ROLE_WITHDRAWN',
               provider = CONCAT('withdrawn_', provider),
               provider_id = CONCAT('withdrawn_', provider_id)
         WHERE id = #{id};

        <choose>
          <when test="role == 'ROLE_BUYER'">
            UPDATE buyers
               SET is_deleted = true,
                   deleted_at = #{deletedAt}
             WHERE user_id = #{id};
          </when>

          <when test="role == 'ROLE_SELLER'">
            UPDATE sellers
               SET is_deleted = true,
                   deleted_at = #{deletedAt}
             WHERE user_id = #{id};
          </when>
        </choose>
        </script>
        """)
    void withdrawUser(@Param("id")  String id,
                      @Param("role")     String role,
                      @Param("deletedAt") OffsetDateTime deletedAt);
}

