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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationCacheServiceTest {

    private static final String USER_ID = "fb20834256d57e6dbe659f7167f7ee12";
    private static final String CACHE_KEY = "security:authorization:user:" + USER_ID;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysPermissionMapper sysPermissionMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    private AuthorizationCacheService cacheService;

    @BeforeEach
    void setUp() {
        QuoteSecurityProperties properties = new QuoteSecurityProperties();
        properties.getAuthorizationCache().setKeyPrefix("security:authorization:user:");
        properties.getAuthorizationCache().setTtlSeconds(600L);
        cacheService = new AuthorizationCacheService(
                redisTemplate, properties, sysUserMapper, sysRoleMapper,
                sysPermissionMapper, sysUserRoleMapper, sysRolePermissionMapper);
    }

    @Test
    void shouldUseCachedAuthorizationWithoutQueryingDatabase() throws Exception {
        AuthorizationSnapshot snapshot = new AuthorizationSnapshot(
                USER_ID, "lin", 1, List.of("salesman"), List.of("quote:create"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn(new ObjectMapper().writeValueAsString(snapshot));

        SecurityUser result = cacheService.getOrLoad(USER_ID, "lin");

        assertEquals(List.of("salesman"), result.getRoles());
        assertEquals(List.of("quote:create"), result.getPermissions());
        assertNull(result.getPassword());
        verify(sysUserMapper, never()).selectById(anyString());
    }

    @Test
    void shouldLoadDatabaseAndPopulateCacheOnMiss() {
        SysUser user = new SysUser();
        user.setId(USER_ID);
        user.setUsername("lin");
        user.setStatus(1);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        when(sysUserMapper.selectById(USER_ID)).thenReturn(user);
        when(sysRoleMapper.selectRoleCodesByUserId(USER_ID)).thenReturn(List.of("salesman"));
        when(sysPermissionMapper.selectPermissionCodesByUserId(USER_ID))
                .thenReturn(List.of("quote:create", "quote:update"));

        SecurityUser result = cacheService.getOrLoad(USER_ID, "lin");

        assertEquals(2, result.getPermissions().size());
        verify(valueOperations).set(
                org.mockito.ArgumentMatchers.eq(CACHE_KEY),
                anyString(),
                org.mockito.ArgumentMatchers.eq(600L),
                org.mockito.ArgumentMatchers.eq(TimeUnit.SECONDS));
    }
}
