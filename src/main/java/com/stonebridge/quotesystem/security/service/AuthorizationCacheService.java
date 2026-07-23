package com.stonebridge.quotesystem.security.service;

import com.stonebridge.quotesystem.security.entity.AuthorizationSnapshot;
import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.security.utils.QuoteSecurityProperties;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.mapper.SysPermissionMapper;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import com.stonebridge.quotesystem.system.mapper.SysRolePermissionMapper;
import com.stonebridge.quotesystem.system.mapper.SysUserMapper;
import com.stonebridge.quotesystem.system.mapper.SysUserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户授权快照缓存。数据库始终是权限真源；Redis 不可用或缓存损坏时自动回源数据库。
 */
@Service
public class AuthorizationCacheService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationCacheService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final QuoteSecurityProperties properties;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthorizationCacheService(StringRedisTemplate stringRedisTemplate,
                                     QuoteSecurityProperties properties,
                                     SysUserMapper sysUserMapper,
                                     SysRoleMapper sysRoleMapper,
                                     SysPermissionMapper sysPermissionMapper,
                                     SysUserRoleMapper sysUserRoleMapper,
                                     SysRolePermissionMapper sysRolePermissionMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.properties = properties;
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
    }

    public SecurityUser getOrLoad(String userId, String expectedUsername) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(expectedUsername)) {
            throw new UsernameNotFoundException("Token缺少用户信息");
        }

        AuthorizationSnapshot snapshot = readSnapshot(userId);
        if (snapshot == null || !isSnapshotUsable(snapshot, userId, expectedUsername)) {
            snapshot = loadFromDatabase(userId, expectedUsername);
            writeSnapshot(snapshot);
        }
        return toSecurityUser(snapshot);
    }

    /** 登录成功后直接复用数据库已经加载出的角色权限，避免下一请求再次回源。 */
    public void cache(SecurityUser securityUser) {
        if (securityUser == null || securityUser.getSysUser() == null) {
            return;
        }
        SysUser user = securityUser.getSysUser();
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                user.getId(), user.getUsername(), user.getStatus(),
                safeList(securityUser.getRoles()), safeList(securityUser.getPermissions()));
        writeSnapshot(snapshot);
    }

    public void evictUserAfterCommit(String userId) {
        if (StringUtils.hasText(userId)) {
            evictUsersAfterCommit(List.of(userId.trim()));
        }
    }

    public void evictUsersByRoleAfterCommit(String roleId) {
        if (!StringUtils.hasText(roleId)) {
            return;
        }
        List<String> userIds = sysUserRoleMapper.selectUserIdsByRoleIds(List.of(roleId.trim()));
        evictUsersAfterCommit(userIds);
    }

    public void evictUsersByPermissionsAfterCommit(Collection<String> permissionIds) {
        List<String> normalizedPermissionIds = normalizeIds(permissionIds);
        if (normalizedPermissionIds.isEmpty()) {
            return;
        }
        List<String> roleIds = sysRolePermissionMapper.selectRoleIdsByPermissionIds(normalizedPermissionIds);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        List<String> userIds = sysUserRoleMapper.selectUserIdsByRoleIds(roleIds);
        evictUsersAfterCommit(userIds);
    }

    private AuthorizationSnapshot readSnapshot(String userId) {
        String key = buildKey(userId);
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, AuthorizationSnapshot.class);
        } catch (Exception exception) {
            // 授权缓存故障不影响登录；回源数据库仍可保证权限准确性。
            log.warn("读取用户授权缓存失败，将回源数据库，userId={}", userId, exception);
            return null;
        }
    }

    private AuthorizationSnapshot loadFromDatabase(String userId, String expectedUsername) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !Objects.equals(expectedUsername, user.getUsername())) {
            throw new UsernameNotFoundException("登录状态已失效");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new DisabledException("账号已被停用");
        }
        return new AuthorizationSnapshot(
                user.getId(), user.getUsername(), user.getStatus(),
                safeList(sysRoleMapper.selectRoleCodesByUserId(userId)),
                safeList(sysPermissionMapper.selectPermissionCodesByUserId(userId)));
    }

    private boolean isSnapshotUsable(AuthorizationSnapshot snapshot,
                                     String userId,
                                     String expectedUsername) {
        return Objects.equals(userId, snapshot.getUserId())
                && Objects.equals(expectedUsername, snapshot.getUsername())
                && Integer.valueOf(1).equals(snapshot.getStatus());
    }

    private SecurityUser toSecurityUser(AuthorizationSnapshot snapshot) {
        SysUser user = new SysUser();
        user.setId(snapshot.getUserId());
        user.setUsername(snapshot.getUsername());
        user.setStatus(snapshot.getStatus());
        return new SecurityUser(user, safeList(snapshot.getRoles()), safeList(snapshot.getPermissions()));
    }

    private void writeSnapshot(AuthorizationSnapshot snapshot) {
        if (snapshot == null || !StringUtils.hasText(snapshot.getUserId())) {
            return;
        }
        Long configuredTtl = properties.getAuthorizationCache().getTtlSeconds();
        long ttlSeconds = configuredTtl == null ? 600L : Math.max(1L, configuredTtl);
        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(snapshot.getUserId()),
                    objectMapper.writeValueAsString(snapshot),
                    ttlSeconds,
                    TimeUnit.SECONDS);
        } catch (Exception exception) {
            // 缓存写入失败不阻断登录或数据库回源结果。
            log.warn("写入用户授权缓存失败，userId={}", snapshot.getUserId(), exception);
        }
    }

    private void evictUsersAfterCommit(Collection<String> userIds) {
        List<String> normalizedUserIds = normalizeIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return;
        }
        Runnable eviction = () -> evictUsers(normalizedUserIds);
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }

    private void evictUsers(Collection<String> userIds) {
        List<String> keys = userIds.stream().map(this::buildKey).collect(Collectors.toList());
        try {
            stringRedisTemplate.delete(keys);
        } catch (RuntimeException exception) {
            // 最长只会保留到授权快照TTL；记录错误以便运维告警。
            log.error("清理用户授权缓存失败，userIds={}", userIds, exception);
        }
    }

    private String buildKey(String userId) {
        return properties.getAuthorizationCache().getKeyPrefix() + userId;
    }

    private List<String> normalizeIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = ids.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(normalized);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.emptyList() : new ArrayList<>(values);
    }
}
