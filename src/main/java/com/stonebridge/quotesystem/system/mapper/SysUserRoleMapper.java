package com.stonebridge.quotesystem.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 批量写入用户角色关联，字段严格对应 quote_auth.sql 的 sys_user_role。
     */
    @Insert({
            "<script>",
            "INSERT INTO sys_user_role (",
            "  id, user_id, role_id, is_deleted,",
            "  create_time, create_by, update_time, update_by",
            ") VALUES",
            "<foreach collection='relations' item='relation' separator=','>",
            "(",
            "  #{relation.id}, #{relation.userId}, #{relation.roleId}, #{relation.isDeleted},",
            "  #{relation.createTime}, #{relation.createBy}, #{relation.updateTime}, #{relation.updateBy}",
            ")",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("relations") List<SysUserRole> relations);

    @Select("""
            SELECT role_id
            FROM sys_user_role
            WHERE user_id = #{userId}
              AND is_deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<String> selectRoleIdsByUserId(@Param("userId") String userId);

    @Select("""
            SELECT user_id
            FROM sys_user_role
            WHERE role_id = #{roleId}
              AND is_deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<String> selectUserIdsByRoleId(@Param("roleId") String roleId);

    @Select({
            "<script>",
            "SELECT DISTINCT user_id",
            "FROM sys_user_role",
            "WHERE is_deleted = 0 AND role_id IN",
            "<foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>",
            "#{roleId}",
            "</foreach>",
            "</script>"
    })
    List<String> selectUserIdsByRoleIds(@Param("roleIds") List<String> roleIds);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") String userId);

}
