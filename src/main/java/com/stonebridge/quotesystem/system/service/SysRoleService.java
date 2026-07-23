package com.stonebridge.quotesystem.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.system.entity.SysRole;
import com.stonebridge.quotesystem.system.entity.dto.RoleSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.SysRoleQueryVo;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    Page<SysRole> pageRoles(Integer page, Integer limit, SysRoleQueryVo queryVo);

    SysRole createRole(RoleSaveDto dto);

    SysRole updateRole(RoleSaveDto dto);

    void removeRole(String roleId);

    List<String> getRoleCodesByUserId(String userId);

}
