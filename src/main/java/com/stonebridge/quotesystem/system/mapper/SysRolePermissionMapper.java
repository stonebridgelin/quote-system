package com.stonebridge.quotesystem.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.system.entity.SysRolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 批量写入角色权限关联，字段严格对应 quote_auth.sql 的 sys_role_permission。
     */
    @Insert({
            "<script>",
            "INSERT INTO sys_role_permission (",
            "  id, role_id, permission_id, is_deleted,",
            "  create_time, create_by, update_time, update_by",
            ") VALUES",
            "<foreach collection='relations' item='relation' separator=','>",
            "(",
            "  #{relation.id}, #{relation.roleId}, #{relation.permissionId}, #{relation.isDeleted},",
            "  #{relation.createTime}, #{relation.createBy}, #{relation.updateTime}, #{relation.updateBy}",
            ")",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("relations") List<SysRolePermission> relations);

    @Select("""
            SELECT permission_id
            FROM sys_role_permission
            WHERE role_id = #{roleId}
              AND is_deleted = 0
            ORDER BY create_time ASC, id ASC
            """)
    List<String> selectPermissionIdsByRoleId(@Param("roleId") String roleId);

    @Delete("DELETE FROM sys_role_permission WHERE role_id = #{roleId}")
    int physicalDeleteByRoleId(@Param("roleId") String roleId);

    @Delete("DELETE FROM sys_role_permission WHERE permission_id = #{permissionId}")
    int physicalDeleteByPermissionId(@Param("permissionId") String permissionId);
}
