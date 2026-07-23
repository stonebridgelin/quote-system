package com.stonebridge.quotesystem.security.entity;

import com.stonebridge.quotesystem.system.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SecurityUser implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final SysUser sysUser;
    private final List<String> roles;
    private final List<String> permissions;

    public SecurityUser(SysUser sysUser, List<String> roles, List<String> permissions) {
        this.sysUser = sysUser;
        this.roles = roles == null ? Collections.emptyList() : roles;
        this.permissions = permissions == null ? Collections.emptyList() : permissions;
    }

    public SysUser getSysUser() {
        return sysUser;
    }

    public String getUserId() {
        return sysUser == null ? null : sysUser.getId();
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public List<String> getAuthorityList() {
        return Stream.concat(
                        roles.stream().filter(Objects::nonNull).map(SecurityUser::normalizeRole),
                        permissions.stream().filter(Objects::nonNull)
                )
                .filter(auth -> auth != null && !auth.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normalizeRole(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return roleCode;
        }
        return roleCode.startsWith("ROLE_") ? roleCode : "ROLE_" + roleCode;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return getAuthorityList().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return sysUser == null ? null : sysUser.getPassword();
    }

    @Override
    public String getUsername() {
        return sysUser == null ? null : sysUser.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return sysUser != null && Integer.valueOf(1).equals(sysUser.getStatus());
    }
}
