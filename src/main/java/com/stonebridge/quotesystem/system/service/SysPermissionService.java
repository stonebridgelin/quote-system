package com.stonebridge.quotesystem.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.system.entity.SysPermission;
import com.stonebridge.quotesystem.system.entity.dto.AssignPermissionDto;
import com.stonebridge.quotesystem.system.entity.dto.PermissionSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.RouterVo;

import java.util.List;

/**
 * 系统权限 Service。
 *
 * 新表：sys_permissions。
 * 旧 SysMenuService / SysMenu 已废弃。
 */
public interface SysPermissionService extends IService<SysPermission> {

    List<SysPermission> findPermissionTree();

    List<SysPermission> findNodes();

    SysPermission createPermission(PermissionSaveDto dto);

    SysPermission updatePermission(PermissionSaveDto dto);

    void removePermission(String permissionId);

    void changeStatus(String permissionId, Integer status);

    boolean hasChildren(String permissionId);

    void assignPermissions(AssignPermissionDto dto);

    List<String> getPermissionCodesByUserId(String userId);

    List<SysPermission> getUserPageRoutes(String userId);

    List<SysPermission> getUserHomeModules(String userId);

    List<RouterVo> getUserRouters(String userId);
}
