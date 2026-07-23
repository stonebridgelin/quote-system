package com.stonebridge.quotesystem.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stonebridge.quotesystem.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT p.permission_code
            FROM sys_permissions p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = 0
              AND rp.is_deleted = 0
              AND p.permission_code IS NOT NULL
              AND p.permission_code <> ''
              AND p.status = 1
              AND p.is_deleted = 0
              AND r.status = 1
              AND r.is_deleted = 0
            GROUP BY p.permission_code
            ORDER BY MIN(p.sort_value) ASC, MIN(p.id) ASC
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") String userId);

    @Select("""
            SELECT DISTINCT
                p.id, p.parent_id, p.permission_name, p.permission_code,
                p.permission_type, p.module_code, p.action_code,
                p.route_path, p.route_name, p.component_path, p.redirect_path,
                p.icon, p.visible, p.keep_alive, p.show_in_home,
                p.home_title, p.home_description, p.button_key,
                p.api_method, p.api_path, p.sort_value, p.status,
                p.remark, p.meta_json, p.is_deleted,
                p.create_time, p.create_by, p.update_time, p.update_by
            FROM sys_permissions p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = 0
              AND rp.is_deleted = 0
              AND p.permission_type = 1
              AND p.status = 1
              AND p.is_deleted = 0
              AND r.status = 1
              AND r.is_deleted = 0
            ORDER BY p.sort_value ASC, p.id ASC
            """)
    List<SysPermission> selectUserPageRoutes(@Param("userId") String userId);

    @Select("""
            SELECT DISTINCT
                p.id, p.parent_id, p.permission_name, p.permission_code,
                p.permission_type, p.module_code, p.action_code,
                p.route_path, p.route_name, p.component_path, p.redirect_path,
                p.icon, p.visible, p.keep_alive, p.show_in_home,
                p.home_title, p.home_description, p.button_key,
                p.api_method, p.api_path, p.sort_value, p.status,
                p.remark, p.meta_json, p.is_deleted,
                p.create_time, p.create_by, p.update_time, p.update_by
            FROM sys_permissions p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            INNER JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
              AND ur.is_deleted = 0
              AND rp.is_deleted = 0
              AND p.permission_type = 1
              AND p.show_in_home = 1
              AND p.visible = 1
              AND p.status = 1
              AND p.is_deleted = 0
              AND r.status = 1
              AND r.is_deleted = 0
            ORDER BY p.sort_value ASC, p.id ASC
            """)
    List<SysPermission> selectUserHomeModules(@Param("userId") String userId);

    @Select("""
            SELECT *
            FROM sys_permissions
            WHERE is_deleted = 0
            ORDER BY sort_value ASC, id ASC
            """)
    List<SysPermission> selectPermissionTreeList();
}
