package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.batch.dto.WithdrawBuyerDTO;
import com.team5.catdogeats.users.domain.Users;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestMapper {

    @Select("""
    SELECT u.id            AS user_id,
           u.role          AS role,
           u.account_disable AS accountDisable,
           u.deleted_at    AS deletedAt,
           b.user_id       AS buyerId,
           b.is_deleted    AS isDeleted,
           b.deleted_at    AS buyerDeletedAt
      FROM users u
      JOIN sellers b ON u.id = b.user_id
     WHERE u.id = #{userId}
       AND u.account_disable = TRUE
""")
    Users findBySellerId(@Param("userId") String user_id);

    @Select("""
    SELECT u.id            AS user_id,
           u.role          AS role,
           u.account_disable AS accountDisable,
           u.deleted_at    AS deletedAt,
           b.user_id       AS buyerId,
           b.is_deleted    AS isDeleted,
           b.deleted_at    AS buyerDeletedAt
      FROM users u
      JOIN buyers b ON u.id = b.user_id
     WHERE u.id = #{userId}
       AND u.account_disable = TRUE
""")
    WithdrawBuyerDTO findByBuyersId(@Param("userId") String userId);


}
