package com.stonebridge.quotesystem.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.security.service.AuthorizationCacheService;
import com.stonebridge.quotesystem.system.entity.SysUserRole;
import com.stonebridge.quotesystem.system.mapper.SysUserRoleMapper;
import com.stonebridge.quotesystem.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole>
        implements SysUserRoleService {

    private final AuthorizationCacheService authorizationCacheService;

    public SysUserRoleServiceImpl(AuthorizationCacheService authorizationCacheService) {
        this.authorizationCacheService = authorizationCacheService;
    }

    @Override
    public List<String> getRoleIdsByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        return baseMapper.selectRoleIdsByUserId(userId.trim());
    }

    @Override
    public List<String> getUserIdsByRoleId(String roleId) {
        if (!StringUtils.hasText(roleId)) {
            return Collections.emptyList();
        }
        return baseMapper.selectUserIdsByRoleId(roleId.trim());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceUserRoles(String userId, List<String> roleIds, String operatorId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        String normalizedUserId = userId.trim();
        baseMapper.physicalDeleteByUserId(normalizedUserId);

        if (roleIds == null || roleIds.isEmpty()) {
            authorizationCacheService.evictUserAfterCommit(normalizedUserId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<SysUserRole> relations = new ArrayList<>();
        for (String roleId : new LinkedHashSet<>(roleIds)) {
            if (!StringUtils.hasText(roleId)) {
                continue;
            }
            SysUserRole relation = new SysUserRole();
            relation.setId(IdWorker.get32UUID());
            relation.setUserId(normalizedUserId);
            relation.setRoleId(roleId.trim());
            relation.setIsDeleted(0);
            relation.setCreateBy(operatorId);
            relation.setUpdateBy(operatorId);
            relation.setCreateTime(now);
            relation.setUpdateTime(now);
            relations.add(relation);
        }

        // 循环只负责组装对象，数据库写入由一条批量 INSERT 完成。
        if (!relations.isEmpty()) {
            baseMapper.insertBatch(relations);
        }
        authorizationCacheService.evictUserAfterCommit(normalizedUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            String normalizedUserId = userId.trim();
            baseMapper.physicalDeleteByUserId(normalizedUserId);
            authorizationCacheService.evictUserAfterCommit(normalizedUserId);
        }
    }

}
