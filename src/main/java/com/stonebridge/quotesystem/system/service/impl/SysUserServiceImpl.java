package com.stonebridge.quotesystem.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.security.service.AuthorizationCacheService;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysRole;
import com.stonebridge.quotesystem.system.entity.SysUser;
import com.stonebridge.quotesystem.system.entity.dto.AssignRoleDto;
import com.stonebridge.quotesystem.system.entity.dto.UserSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.UserQueryVo;
import com.stonebridge.quotesystem.system.entity.vo.UserRoleVo;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import com.stonebridge.quotesystem.system.mapper.SysUserMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import com.stonebridge.quotesystem.system.service.SysUserRoleService;
import com.stonebridge.quotesystem.system.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleService userRoleService;
    private final SysPermissionService permissionService;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationCacheService authorizationCacheService;

    public SysUserServiceImpl(SysRoleMapper sysRoleMapper,
                              SysUserRoleService userRoleService,
                              SysPermissionService permissionService,
                              PasswordEncoder passwordEncoder,
                              AuthorizationCacheService authorizationCacheService) {
        this.sysRoleMapper = sysRoleMapper;
        this.userRoleService = userRoleService;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
        this.authorizationCacheService = authorizationCacheService;
    }

    @Override
    public Page<SysUser> pageUsers(Integer page, Integer limit, UserQueryVo queryVo) {
        Page<SysUser> pageParam = new Page<>(normalizePage(page), normalizeLimit(limit));
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (queryVo != null) {
            if (StringUtils.hasText(queryVo.getKeyword())) {
                String keyword = queryVo.getKeyword().trim();
                wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getNickname, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword)
                        .or().like(SysUser::getEmail, keyword));
            }
            if (queryVo.getStatus() != null) {
                wrapper.eq(SysUser::getStatus, queryVo.getStatus());
            }
            if (StringUtils.hasText(queryVo.getCreateTimeBegin())) {
                wrapper.ge(SysUser::getCreateTime, queryVo.getCreateTimeBegin().trim());
            }
            if (StringUtils.hasText(queryVo.getCreateTimeEnd())) {
                wrapper.le(SysUser::getCreateTime, queryVo.getCreateTimeEnd().trim());
            }
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = page(pageParam, wrapper);
        result.getRecords().forEach(this::erasePassword);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(UserSaveDto dto) {
        validateCreateUser(dto);
        String username = dto.getUsername().trim();
        if (baseMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        List<String> roleIds = normalizeAndValidateRoleIds(dto.getRoleIds());
        LocalDateTime now = LocalDateTime.now();
        String operatorId = currentOperatorId();
        SysUser user = new SysUser();
        copyDtoToUser(dto, user, true);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        user.setCreateBy(operatorId);
        user.setUpdateBy(operatorId);
        user.setCreateTime(now);
        user.setUpdateTime(now);
        save(user);

        userRoleService.replaceUserRoles(user.getId(), roleIds, operatorId);
        erasePassword(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(UserSaveDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        String userId = dto.getId().trim();
        SysUser user = getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (StringUtils.hasText(dto.getUsername())) {
            String newUsername = dto.getUsername().trim();
            SysUser sameUsernameUser = baseMapper.selectByUsername(newUsername);
            if (sameUsernameUser != null && !userId.equals(sameUsernameUser.getId())) {
                throw new IllegalArgumentException("用户名已存在");
            }
        }

        if (dto.getStatus() != null) {
            validateStatus(dto.getStatus());
        }
        List<String> roleIds = dto.getRoleIds() == null
                ? null
                : normalizeAndValidateRoleIds(dto.getRoleIds());

        copyDtoToUser(dto, user, false);
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword().trim()));
        }
        String operatorId = currentOperatorId();
        user.setUpdateBy(operatorId);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        if (roleIds != null) {
            userRoleService.replaceUserRoles(userId, roleIds, operatorId);
        }
        authorizationCacheService.evictUserAfterCommit(userId);
        erasePassword(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String id = userId.trim();
        SysUser user = getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String currentUserId = currentOperatorId();
        if (StringUtils.hasText(currentUserId) && currentUserId.equals(id)) {
            throw new IllegalArgumentException("不能删除当前登录账号");
        }

        userRoleService.removeByUserId(id);
        removeById(id);
    }

    @Override
    public void changeStatus(String userId, Integer status) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        validateStatus(status);
        SysUser user = getById(userId.trim());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setStatus(status);
        user.setUpdateBy(currentOperatorId());
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        authorizationCacheService.evictUserAfterCommit(user.getId());
    }

    @Override
    public void resetPassword(String userId, String newPassword) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("用户ID和新密码不能为空");
        }
        SysUser user = getById(userId.trim());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        user.setUpdateBy(currentOperatorId());
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        authorizationCacheService.evictUserAfterCommit(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignRoleDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getUserId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String userId = dto.getUserId().trim();
        if (getById(userId) == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<String> roleIds = normalizeAndValidateRoleIds(dto.getRoleIds());
        userRoleService.replaceUserRoles(userId, roleIds, currentOperatorId());
    }

    @Override
    public UserRoleVo getRolesByUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String id = userId.trim();
        UserRoleVo vo = new UserRoleVo();
        vo.setUserId(id);
        vo.setAllRoles(sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getSortValue)));
        vo.setAssignedRoleIds(userRoleService.getRoleIdsByUserId(id));
        return vo;
    }

    @Override
    public Map<String, Object> getUserDetail(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        String id = userId.trim();
        SysUser user = getById(id);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        erasePassword(user);
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roleIds", userRoleService.getRoleIdsByUserId(id));
        result.put("roles", sysRoleMapper.selectRoleCodesByUserId(id));
        result.put("permissions", permissionService.getPermissionCodesByUserId(id));
        result.put("routes", permissionService.getUserPageRoutes(id));
        result.put("homeModules", permissionService.getUserHomeModules(id));
        return result;
    }

    private List<String> normalizeAndValidateRoleIds(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String roleId : roleIds) {
            if (StringUtils.hasText(roleId)) {
                normalized.add(roleId.trim());
            }
        }
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysRole> roles = sysRoleMapper.selectBatchIds(normalized);
        if (roles.size() != normalized.size()) {
            throw new IllegalArgumentException("部分角色不存在或已删除");
        }
        for (SysRole role : roles) {
            if (!Integer.valueOf(1).equals(role.getStatus())) {
                throw new IllegalArgumentException("不能分配已停用角色：" + role.getRoleName());
            }
        }
        return new ArrayList<>(normalized);
    }

    private void validateCreateUser(UserSaveDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        if (!StringUtils.hasText(dto.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        validateStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private void copyDtoToUser(UserSaveDto dto, SysUser user, boolean create) {
        if (StringUtils.hasText(dto.getUsername())) {
            user.setUsername(dto.getUsername().trim());
        }
        user.setNickname(trimToNull(dto.getNickname()));
        user.setRealName(trimToNull(dto.getRealName()));
        user.setGender(dto.getGender() == null ? (user.getGender() == null ? 2 : user.getGender()) : dto.getGender());
        user.setPhone(trimToNull(dto.getPhone()));
        user.setEmail(trimToNull(dto.getEmail()));
        user.setAvatar(trimToNull(dto.getAvatar()));
        user.setUserType(dto.getUserType() == null ? (user.getUserType() == null ? 1 : user.getUserType()) : dto.getUserType());
        user.setStatus(dto.getStatus() == null ? (user.getStatus() == null ? 1 : user.getStatus()) : dto.getStatus());
        user.setRemark(trimToNull(dto.getRemark()));
        if (create) {
            user.setIsDeleted(0);
        }
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("用户状态只能为0或1");
        }
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

    private void erasePassword(SysUser user) {
        if (user != null) {
            user.setPassword(null);
        }
    }
}
