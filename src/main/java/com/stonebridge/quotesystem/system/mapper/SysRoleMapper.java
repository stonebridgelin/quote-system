package com.stonebridge.quotesystem.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = 0
              AND r.role_code IS NOT NULL
              AND r.role_code <> ''
              AND r.status = 1
              AND r.is_deleted = 0
            GROUP BY r.role_code
            ORDER BY MIN(r.sort_value) ASC, MIN(r.id) ASC
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") String userId);

}
