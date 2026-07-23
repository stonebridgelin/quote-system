package com.stonebridge.quotesystem.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.system.entity.SysRolePermission;

import java.util.List;

public interface SysRolePermissionService extends IService<SysRolePermission> {
    void replaceRolePermissions(String roleId, List<String> permissionIds, String operatorId);
    void removeByRoleId(String roleId);
    void removeByPermissionId(String permissionId);
}
