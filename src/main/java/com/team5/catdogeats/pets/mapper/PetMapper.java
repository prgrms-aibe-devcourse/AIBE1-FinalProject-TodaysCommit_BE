package com.team5.catdogeats.pets.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

@Mapper
public interface PetMapper {

    @Delete("""
        DELETE FROM pets
         WHERE user_id IN (
               SELECT user_id
                 FROM buyers
                WHERE deleted_at <= #{limit}
         )
    """)
    int deletePetsByWithdrawnBefore(@Param("limit") OffsetDateTime limit);
}
