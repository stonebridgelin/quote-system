package com.stonebridge.quotesystem.security.service;

import com.stonebridge.quotesystem.system.mapper.SysPermissionMapper;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurityPermissionService {
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SecurityPermissionService(SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }
}
