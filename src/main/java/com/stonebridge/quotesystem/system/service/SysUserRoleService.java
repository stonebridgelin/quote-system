package com.stonebridge.quotesystem.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.system.entity.SysUserRole;

import java.util.List;

public interface SysUserRoleService extends IService<SysUserRole> {
    List<String> getRoleIdsByUserId(String userId);
    List<String> getUserIdsByRoleId(String roleId);
    void replaceUserRoles(String userId, List<String> roleIds, String operatorId);
    void removeByUserId(String userId);
}
