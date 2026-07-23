package com.stonebridge.quotesystem.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.system.entity.SysRole;
import com.stonebridge.quotesystem.system.entity.dto.AssignPermissionDto;
import com.stonebridge.quotesystem.system.entity.dto.RoleSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.SysRoleQueryVo;
import com.stonebridge.quotesystem.system.mapper.SysRolePermissionMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import com.stonebridge.quotesystem.system.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public SysRoleController(SysRoleService sysRoleService,
                             SysPermissionService sysPermissionService,
                             SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    @PreAuthorize("hasAnyAuthority('system:role:page','system:role:list')")
    @GetMapping("/{page}/{limit}")
    public Result<Page<SysRole>> page(@PathVariable Integer page,
                                      @PathVariable Integer limit,
                                      SysRoleQueryVo queryVo) {
        return Result.success(sysRoleService.pageRoles(page, limit, queryVo));
    }

    /**
     * 用户分配角色时使用。
     */
    @PreAuthorize("hasAnyAuthority('system:role:page','system:role:list','system:user:assign-role','system:user:assignRole')")
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.success(sysRoleService.list());
    }

    @PreAuthorize("hasAnyAuthority('system:role:query','system:role:page','system:role:list')")
    @GetMapping("/detail/{id}")
    public Result<SysRole> getById(@PathVariable String id) {
        return Result.success(sysRoleService.getById(id));
    }

    @PreAuthorize("hasAnyAuthority('system:role:create','system:role:add')")
    @PostMapping
    public Result<SysRole> create(@RequestBody RoleSaveDto dto) {
        return Result.success(sysRoleService.createRole(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:role:update','system:role:edit')")
    @PutMapping
    public Result<SysRole> update(@RequestBody RoleSaveDto dto) {
        return Result.success(sysRoleService.updateRole(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:role:delete')")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable String id) {
        sysRoleService.removeRole(id);
        return Result.success();
    }

    /**
     * 角色分配权限前的数据准备。
     * 返回全部权限树和当前角色已选权限ID。
     */
    @PreAuthorize("hasAnyAuthority('system:role:assign-permission','system:role:assignMenu','system:role:query')")
    @GetMapping("/{roleId}/permissions")
    public Result<Map<String, Object>> permissions(@PathVariable String roleId) {
        Map<String, Object> data = new HashMap<>();
        data.put("permissionTree", sysPermissionService.findPermissionTree());
        data.put("assignedPermissionIds", sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId));
        return Result.success(data);
    }

    /**
     * 给角色分配权限。
     */
    @PreAuthorize("hasAnyAuthority('system:role:assign-permission','system:role:assignMenu')")
    @PostMapping("/assignPermissions")
    public Result<Object> assignPermissions(@RequestBody AssignPermissionDto dto) {
        sysPermissionService.assignPermissions(dto);
        return Result.success();
    }
}
