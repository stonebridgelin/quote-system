package com.stonebridge.quotesystem.system.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysPermission;
import com.stonebridge.quotesystem.system.entity.dto.AssignPermissionDto;
import com.stonebridge.quotesystem.system.entity.dto.ChangeStatusDto;
import com.stonebridge.quotesystem.system.entity.dto.PermissionSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.RouterVo;
import com.stonebridge.quotesystem.system.mapper.SysRolePermissionMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统权限控制器。
 * <p>
 * 新表：sys_permissions。
 * 权限类型：1 页面权限；2 按钮/接口权限。
 */
@RestController
@RequestMapping("/system/permission")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;
    private final SysRolePermissionMapper sysRolePermissionMapper;

    public SysPermissionController(SysPermissionService sysPermissionService,
                                   SysRolePermissionMapper sysRolePermissionMapper) {
        this.sysPermissionService = sysPermissionService;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    /**
     * 权限管理页：查询全部权限树。
     */
    @PreAuthorize("hasAnyAuthority('system:permission:page','system:permission:list','system:menu:list')")
    @GetMapping("/tree")
    public Result<List<SysPermission>> tree() {
        return Result.success(sysPermissionService.findPermissionTree());
    }

    /**
     * 权限树节点。保留给前端树形选择使用。
     */
    @PreAuthorize("hasAnyAuthority('system:permission:page','system:permission:list','system:menu:list')")
    @GetMapping("/nodes")
    public Result<List<SysPermission>> nodes() {
        return Result.success(sysPermissionService.findNodes());
    }

    /**
     * 当前登录用户可访问的动态路由。
     * 普通业务用户也需要调用，所以只要求登录，不加 system:* 管理权限。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/routes")
    public Result<List<RouterVo>> routes() {
        String userId = currentUserId();
        return Result.success(sysPermissionService.getUserRouters(userId));
    }

    /**
     * 当前登录用户可访问的页面权限原始数据。
     * 前端动态注册路由时也可以直接使用该接口。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/page-routes")
    public Result<List<SysPermission>> pageRoutes() {
        String userId = currentUserId();
        return Result.success(sysPermissionService.getUserPageRoutes(userId));
    }

    /**
     * 当前登录用户业务首页模块。
     * Home.vue 应根据这里返回的数据渲染业务入口。
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/home-modules")
    public Result<List<SysPermission>> homeModules() {
        String userId = currentUserId();
        return Result.success(sysPermissionService.getUserHomeModules(userId));
    }

    @PreAuthorize("hasAnyAuthority('system:permission:query','system:permission:page','system:menu:query')")
    @GetMapping("/{id}")
    public Result<SysPermission> getById(@PathVariable String id) {
        return Result.success(sysPermissionService.getById(id));
    }

    @PreAuthorize("hasAnyAuthority('system:permission:create','system:permission:add','system:menu:add')")
    @PostMapping
    public Result<SysPermission> create(@RequestBody PermissionSaveDto dto) {
        return Result.success(sysPermissionService.createPermission(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:permission:update','system:permission:edit','system:menu:edit')")
    @PutMapping
    public Result<SysPermission> update(@RequestBody PermissionSaveDto dto) {
        return Result.success(sysPermissionService.updatePermission(dto));
    }

    @PreAuthorize("hasAnyAuthority('system:permission:delete','system:menu:delete')")
    @DeleteMapping("/{id}")
    public Result<Object> delete(@PathVariable String id) {
        sysPermissionService.removePermission(id);
        return Result.success();
    }

    @PreAuthorize("hasAnyAuthority('system:permission:update','system:permission:edit','system:permission:change-status','system:menu:edit')")
    @PutMapping("/status")
    public Result<Object> changeStatus(@RequestBody ChangeStatusDto dto) {
        sysPermissionService.changeStatus(dto.getId(), dto.getStatus());
        return Result.success();
    }

    /**
     * 角色分配权限前的数据准备：全部权限树 + 该角色已选权限ID。
     */
    @PreAuthorize("hasAnyAuthority('system:role:query','system:role:assign-permission','system:role:assignMenu')")
    @GetMapping("/to-assign/{roleId}")
    public Result<Map<String, Object>> toAssign(@PathVariable String roleId) {
        List<SysPermission> permissionTree = sysPermissionService.findPermissionTree();
        List<String> queriedPermissionIds = sysRolePermissionMapper.selectPermissionIdsByRoleId(roleId);
        List<String> assignedPermissionIds = queriedPermissionIds == null ? List.of() : queriedPermissionIds;
        Set<String> assignedIdSet = new HashSet<>(assignedPermissionIds);
        List<String> checkedPermissionIds = new ArrayList<>();

        markSelectedNodes(permissionTree, assignedIdSet, checkedPermissionIds);

        Map<String, Object> data = new HashMap<>();
        data.put("permissionTree", permissionTree);
        // 保留原字段，避免影响已有调用方。
        data.put("assignedPermissionIds", assignedPermissionIds);
        // Element Plus 非严格树应使用叶子节点回显，父节点由子节点自动形成全选/半选状态。
        data.put("checkedPermissionIds", checkedPermissionIds);
        return Result.success(data);
    }

    private void markSelectedNodes(List<SysPermission> nodes,
                                   Set<String> assignedPermissionIds,
                                   List<String> checkedPermissionIds) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (SysPermission node : nodes) {
            boolean selected = node.getId() != null && assignedPermissionIds.contains(node.getId());
            node.setSelected(selected);

            List<SysPermission> children = node.getChildren();
            boolean leaf = children == null || children.isEmpty();
            if (selected && leaf) {
                checkedPermissionIds.add(node.getId());
            }
            markSelectedNodes(children, assignedPermissionIds, checkedPermissionIds);
        }
    }

    /**
     * 给角色分配权限。
     */
    @PreAuthorize("hasAnyAuthority('system:role:assign-permission','system:role:assignMenu')")
    @PostMapping("/assign-to-role")
    public Result<Object> assignPermissions(@RequestBody AssignPermissionDto dto) {
        sysPermissionService.assignPermissions(dto);
        return Result.success();
    }

    /**
     * 兼容旧前端接口名。前端改完后可以删除。
     */
    @Deprecated
    @PreAuthorize("hasAnyAuthority('system:role:assign-permission','system:role:assignMenu')")
    @PostMapping("/doAssign")
    public Result<Object> doAssign(@RequestBody AssignPermissionDto dto) {
        sysPermissionService.assignPermissions(dto);
        return Result.success();
    }

    private String currentUserId() {
        return SecurityUtil.getCurrentUserId();
    }
}
