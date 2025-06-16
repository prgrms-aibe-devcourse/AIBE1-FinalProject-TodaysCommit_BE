package com.team5.catdogeats.users.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("""
        SELECT 1+1
    """)
    int selectOne();
}
