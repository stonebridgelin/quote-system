package com.stonebridge.quotesystem.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
            SELECT *
            FROM sys_user
            WHERE username = #{username}
              AND is_deleted = 0
            LIMIT 1
            """)
    SysUser selectByUsername(@Param("username") String username);

    @Update("""
            UPDATE sys_user
            SET last_login_time = #{lastLoginTime},
                last_login_ip = #{lastLoginIp},
                update_time = NOW()
            WHERE id = #{userId}
              AND is_deleted = 0
            """)
    int updateLoginInfo(@Param("userId") String userId,
                        @Param("lastLoginTime") LocalDateTime lastLoginTime,
                        @Param("lastLoginIp") String lastLoginIp);
}
