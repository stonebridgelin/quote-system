package com.stonebridge.quotesystem.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.entity.dto.AssignRoleDto;
import com.stonebridge.quotesystem.system.entity.dto.UserSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.UserQueryVo;
import com.stonebridge.quotesystem.system.entity.vo.UserRoleVo;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {

    Page<SysUser> pageUsers(Integer page, Integer limit, UserQueryVo queryVo);

    SysUser createUser(UserSaveDto dto);

    SysUser updateUser(UserSaveDto dto);

    void removeUser(String userId);

    void changeStatus(String userId, Integer status);

    void resetPassword(String userId, String newPassword);

    void assignRoles(AssignRoleDto dto);

    UserRoleVo getRolesByUserId(String userId);

    Map<String, Object> getUserDetail(String userId);
}
