package com.stonebridge.quotesystem.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.entity.dto.AssignRoleDto;
import com.stonebridge.quotesystem.system.entity.dto.ChangeStatusDto;
import com.stonebridge.quotesystem.system.entity.dto.ResetPasswordDto;
import com.stonebridge.quotesystem.system.entity.dto.UserSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.UserInfoVo;
import com.stonebridge.quotesystem.system.entity.vo.UserQueryVo;
import com.stonebridge.quotesystem.system.entity.vo.UserRoleVo;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import com.stonebridge.quotesystem.system.service.SysRoleService;
import com.stonebridge.quotesystem.system.service.SysUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    public SysUserController(SysUserService sysUserService,
                             SysRoleService sysRoleService,
                             SysPermissionService sysPermissionService) {
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
    }

    @PreAuthorize("hasAnyAuthority('system:user:page','system:user:list')")
    @GetMapping("/{page}/{limit}")
    public Result<Page<SysUser>> page(@PathVariable Integer page,
                                      @PathVariable Integer limit,
                                      UserQueryVo queryVo) {
        return Result.success(sysUserService.pageUsers(page, limit, queryVo));
    }

    @PreAuthorize("hasAnyAuthority('system:user:query','system:user:page','system:user:list')")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getById(@PathVariable String id) {
        return Result.success(sysUserService.getUserDetail(id));
    }

    @PreAuthorize("hasAnyAuthority('system:user:create','system:user:add')")
    @PostMapping
    public Result<SysUser> create(@RequestBody UserSaveDto dto) {
        return Result.success(sysUserService.createUser(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:user:update','system:user:edit')")
    @PutMapping
    public Result<SysUser> update(@RequestBody UserSaveDto dto) {
        return Result.success(sysUserService.updateUser(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:user:delete')")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable String id) {
        sysUserService.removeUser(id);
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('system:user:update','system:user:edit','system:user:change-status')")
    @PutMapping("/status")
    public Result<Object> changeStatus(@RequestBody ChangeStatusDto dto) {
        sysUserService.changeStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('system:user:reset-password','system:user:resetPwd')")
    @PutMapping("/password")
    public Result<Object> resetPassword(@RequestBody ResetPasswordDto dto) {
        sysUserService.resetPassword(dto.getUserId(), dto.getNewPassword());
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('system:user:query','system:user:assign-role','system:user:assignRole')")
    @GetMapping("/{userId}/roles")
    public Result<UserRoleVo> getRoles(@PathVariable String userId) {
        return Result.success(sysUserService.getRolesByUserId(userId));
    }

    @PreAuthorize("hasAnyAuthority('system:user:assign-role','system:user:assignRole')")
    @PostMapping("/assignRoles")
    public Result<Object> assignRoles(@RequestBody AssignRoleDto dto) {
        sysUserService.assignRoles(dto);
        return Result.success();
    }

    /**
     * 当前登录用户信息。
     * 普通业务用户和管理员都会调用，所以只要求登录，不加 system:* 管理权限。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public Result<UserInfoVo> info() {
        SecurityUser securityUser = SecurityUtil.getCurrentUser();
        SysUser user = securityUser.getSysUser();
        String userId = user.getId();

        UserInfoVo vo = new UserInfoVo();
        vo.setId(userId);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(sysRoleService.getRoleCodesByUserId(userId));
        vo.setPermissions(sysPermissionService.getPermissionCodesByUserId(userId));
        vo.setRoutes(sysPermissionService.getUserPageRoutes(userId));
        vo.setHomeModules(sysPermissionService.getUserHomeModules(userId));
        return Result.success(vo);
    }
}
