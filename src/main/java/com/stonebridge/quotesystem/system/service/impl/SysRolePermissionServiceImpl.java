package com.stonebridge.quotesystem.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.system.entity.SysRolePermission;
import com.stonebridge.quotesystem.system.mapper.SysRolePermissionMapper;
import com.stonebridge.quotesystem.system.service.SysRolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, SysRolePermission>
        implements SysRolePermissionService {


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRolePermissions(String roleId, List<String> permissionIds, String operatorId) {
        if (!StringUtils.hasText(roleId)) {
            throw new IllegalArgumentException("角色ID不能为空");
        }

        String normalizedRoleId = roleId.trim();
        baseMapper.physicalDeleteByRoleId(normalizedRoleId);

        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<SysRolePermission> relations = new ArrayList<>();
        for (String permissionId : new LinkedHashSet<>(permissionIds)) {
            if (!StringUtils.hasText(permissionId)) {
                continue;
            }
            SysRolePermission relation = new SysRolePermission();
            relation.setId(IdWorker.get32UUID());
            relation.setRoleId(normalizedRoleId);
            relation.setPermissionId(permissionId.trim());
            relation.setIsDeleted(0);
            relation.setCreateBy(operatorId);
            relation.setUpdateBy(operatorId);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relations.add(relation);
        }

        // 避免每个权限执行一次 INSERT，统一批量写入关联表。
        if (!relations.isEmpty()) {
            baseMapper.insertBatch(relations);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByRoleId(String roleId) {
        if (StringUtils.hasText(roleId)) {
            baseMapper.physicalDeleteByRoleId(roleId.trim());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByPermissionId(String permissionId) {
        if (StringUtils.hasText(permissionId)) {
            baseMapper.physicalDeleteByPermissionId(permissionId.trim());
        }
    }
}
