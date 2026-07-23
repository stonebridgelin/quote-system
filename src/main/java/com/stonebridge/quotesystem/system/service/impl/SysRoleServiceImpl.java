package com.stonebridge.quotesystem.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysRole;
import com.stonebridge.quotesystem.system.entity.dto.AssignPermissionDto;
import com.stonebridge.quotesystem.system.entity.dto.RoleSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.SysRoleQueryVo;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import com.stonebridge.quotesystem.system.service.SysRolePermissionService;
import com.stonebridge.quotesystem.system.service.SysRoleService;
import com.stonebridge.quotesystem.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysRolePermissionService rolePermissionService;
    private final SysUserRoleService userRoleService;
    private final SysPermissionService permissionService;

    public SysRoleServiceImpl(SysRolePermissionService rolePermissionService,
                              SysUserRoleService userRoleService,
                              SysPermissionService permissionService) {
        this.rolePermissionService = rolePermissionService;
        this.userRoleService = userRoleService;
        this.permissionService = permissionService;
    }

    @Override
    public Page<SysRole> pageRoles(Integer page, Integer limit, SysRoleQueryVo queryVo) {
        Page<SysRole> pageParam = new Page<>(normalizePage(page), normalizeLimit(limit));
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (queryVo != null) {
            if (StringUtils.hasText(queryVo.getKeyword())) {
                String keyword = queryVo.getKeyword().trim();
                wrapper.and(w -> w.like(SysRole::getRoleName, keyword)
                        .or().like(SysRole::getRoleCode, keyword)
                        .or().like(SysRole::getDescription, keyword)
                        .or().like(SysRole::getRemark, keyword));
            }
            if (queryVo.getStatus() != null) {
                wrapper.eq(SysRole::getStatus, queryVo.getStatus());
            }
        }
        wrapper.orderByAsc(SysRole::getSortValue).orderByDesc(SysRole::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole createRole(RoleSaveDto dto) {
        validateRole(dto);
        String roleCode = dto.getRoleCode().trim();
        if (existsRoleCode(roleCode, null)) {
            throw new IllegalArgumentException("角色编码已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorId = currentOperatorId();
        SysRole role = new SysRole();
        copyDtoToRole(dto, role, true);
        role.setCreateBy(operatorId);
        role.setUpdateBy(operatorId);
        role.setCreateTime(now);
        role.setUpdateTime(now);
        save(role);

        List<String> permissionIds = resolvePermissionIds(dto);
        if (permissionIds != null) {
            permissionService.assignPermissions(toAssignPermissionDto(role.getId(), permissionIds));
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole updateRole(RoleSaveDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getId())) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        validateRole(dto);

        String roleId = dto.getId().trim();
        SysRole role = getById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (existsRoleCode(dto.getRoleCode().trim(), roleId)) {
            throw new IllegalArgumentException("角色编码已存在");
        }

        copyDtoToRole(dto, role, false);
        role.setUpdateBy(currentOperatorId());
        role.setUpdateTime(LocalDateTime.now());
        updateById(role);

        List<String> permissionIds = resolvePermissionIds(dto);
        if (permissionIds != null) {
            permissionService.assignPermissions(toAssignPermissionDto(roleId, permissionIds));
        }
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(String roleId) {
        if (!StringUtils.hasText(roleId)) {
            throw new IllegalArgumentException("角色ID不能为空");
        }
        String id = roleId.trim();
        SysRole role = getById(id);
        if (role == null) {
            throw new IllegalArgumentException("角色不存在");
        }
        if (!userRoleService.getUserIdsByRoleId(id).isEmpty()) {
            throw new IllegalArgumentException("该角色已分配给用户，不能直接删除");
        }

        rolePermissionService.removeByRoleId(id);
        removeById(id);
    }

    @Override
    public List<String> getRoleCodesByUserId(String userId) {
        return StringUtils.hasText(userId)
                ? baseMapper.selectRoleCodesByUserId(userId.trim())
                : Collections.emptyList();
    }

    private void validateRole(RoleSaveDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("角色信息不能为空");
        }
        if (!StringUtils.hasText(dto.getRoleName())) {
            throw new IllegalArgumentException("角色名称不能为空");
        }
        if (!StringUtils.hasText(dto.getRoleCode())) {
            throw new IllegalArgumentException("角色编码不能为空");
        }
        Integer status = dto.getStatus() == null ? 1 : dto.getStatus();
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("角色状态只能为0或1");
        }
    }

    private boolean existsRoleCode(String roleCode, String excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode);
        if (StringUtils.hasText(excludeId)) {
            wrapper.ne(SysRole::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void copyDtoToRole(RoleSaveDto dto, SysRole role, boolean create) {
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleCode(dto.getRoleCode().trim());
        role.setDescription(trimToNull(dto.getDescription()));
        role.setSortValue(dto.getSortValue() == null ? 0 : dto.getSortValue());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setRemark(trimToNull(dto.getRemark()));
        if (create) {
            role.setIsDeleted(0);
        }
    }

    private List<String> resolvePermissionIds(RoleSaveDto dto) {
        return dto.getPermissionIds();
    }

    private AssignPermissionDto toAssignPermissionDto(String roleId, List<String> permissionIds) {
        AssignPermissionDto dto = new AssignPermissionDto();
        dto.setRoleId(roleId);
        dto.setPermissionIds(permissionIds);
        return dto;
    }

    private long normalizePage(Integer page) {
        return page == null || page < 1 ? 1L : page.longValue();
    }

    private long normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 10L;
        }
        return Math.min(limit, 200);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String currentOperatorId() {
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
