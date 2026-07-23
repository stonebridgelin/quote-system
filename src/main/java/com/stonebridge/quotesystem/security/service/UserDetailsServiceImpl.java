package com.stonebridge.quotesystem.security.service;

import com.stonebridge.quotesystem.security.entity.SecurityUser;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.mapper.SysPermissionMapper;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import com.stonebridge.quotesystem.system.mapper.SysUserMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public UserDetailsServiceImpl(SysUserMapper sysUserMapper,
                                  SysRoleMapper sysRoleMapper,
                                  SysPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new DisabledException("账号已被停用");
        }
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getId());
        List<String> permissions = sysPermissionMapper.selectPermissionCodesByUserId(user.getId());
        return new SecurityUser(user, roles, permissions);
    }
}
