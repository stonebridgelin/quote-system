package com.stonebridge.quotesystem.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stonebridge.quotesystem.security.service.AuthorizationCacheService;
import com.stonebridge.quotesystem.security.utils.SecurityUtil;
import com.stonebridge.quotesystem.system.entity.SysPermission;
import com.stonebridge.quotesystem.system.entity.SysRole;
import com.stonebridge.quotesystem.system.entity.dto.AssignPermissionDto;
import com.stonebridge.quotesystem.system.entity.dto.PermissionSaveDto;
import com.stonebridge.quotesystem.system.entity.vo.MetaVo;
import com.stonebridge.quotesystem.system.entity.vo.RouterVo;
import com.stonebridge.quotesystem.system.mapper.SysPermissionMapper;
import com.stonebridge.quotesystem.system.mapper.SysRoleMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import com.stonebridge.quotesystem.system.service.SysRolePermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements SysPermissionService {

    private static final String ROOT_PARENT_ID = "0";
    private static final int TYPE_PAGE = 1;
    private static final int TYPE_BUTTON = 2;

    private final SysRolePermissionService rolePermissionService;
    private final SysRoleMapper sysRoleMapper;
    private final AuthorizationCacheService authorizationCacheService;

    public SysPermissionServiceImpl(SysRolePermissionService rolePermissionService,
                                    SysRoleMapper sysRoleMapper,
                                    AuthorizationCacheService authorizationCacheService) {
        this.rolePermissionService = rolePermissionService;
        this.sysRoleMapper = sysRoleMapper;
        this.authorizationCacheService = authorizationCacheService;
    }

    @Override
    public List<SysPermission> findPermissionTree() {
        return buildTree(baseMapper.selectPermissionTreeList());
    }

    @Override
    public List<SysPermission> findNodes() {
        return findPermissionTree();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermission createPermission(PermissionSaveDto dto) {
        validatePermission(dto, null);
        String permissionCode = resolvePermissionCode(dto).trim();
        if (existsPermissionCode(permissionCode, null)) {
            throw new IllegalArgumentException("权限标识已存在");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorId = currentOperatorId();
        SysPermission permission = new SysPermission();
        copyDtoToPermission(dto, permission, true);
        permission.setCreateBy(operatorId);
        permission.setUpdateBy(operatorId);
        permission.setCreateTime(now);
        permission.setUpdateTime(now);
        save(permission);
        return permission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermission updatePermission(PermissionSaveDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getId())) {
            throw new IllegalArgumentException("权限ID不能为空");
        }

        String permissionId = dto.getId().trim();
        SysPermission permission = getById(permissionId);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        validatePermission(dto, permissionId);
        String permissionCode = resolvePermissionCode(dto).trim();
        if (existsPermissionCode(permissionCode, permissionId)) {
            throw new IllegalArgumentException("权限标识已存在");
        }

        copyDtoToPermission(dto, permission, false);
        permission.setUpdateBy(currentOperatorId());
        permission.setUpdateTime(LocalDateTime.now());
        updateById(permission);
        authorizationCacheService.evictUsersByPermissionsAfterCommit(List.of(permissionId));
        return permission;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePermission(String permissionId) {
        if (!StringUtils.hasText(permissionId)) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        String id = permissionId.trim();
        SysPermission permission = getById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        if (hasChildren(id)) {
            throw new IllegalArgumentException("该权限存在子权限，请先删除子权限");
        }

        rolePermissionService.removeByPermissionId(id);
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(String permissionId, Integer status) {
        if (!StringUtils.hasText(permissionId)) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        validateStatus(status);

        String id = permissionId.trim();
        SysPermission permission = getById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        String operatorId = currentOperatorId();
        LocalDateTime now = LocalDateTime.now();
        List<String> affectedPermissionIds = new ArrayList<>();
        affectedPermissionIds.add(id);
        if (Integer.valueOf(TYPE_PAGE).equals(permission.getPermissionType())) {
            list(new LambdaQueryWrapper<SysPermission>()
                    .select(SysPermission::getId)
                    .eq(SysPermission::getParentId, id)).stream()
                    .map(SysPermission::getId)
                    .filter(StringUtils::hasText)
                    .forEach(affectedPermissionIds::add);
        }
        permission.setStatus(status);
        permission.setUpdateBy(operatorId);
        permission.setUpdateTime(now);
        updateById(permission);

        // 页面停用时同步停用其按钮/接口权限；重新启用页面时不擅自恢复此前单独停用的按钮。
        if (Integer.valueOf(TYPE_PAGE).equals(permission.getPermissionType()) && status == 0) {
            lambdaUpdate()
                    .eq(SysPermission::getParentId, id)
                    .set(SysPermission::getStatus, 0)
                    .set(SysPermission::getUpdateBy, operatorId)
                    .set(SysPermission::getUpdateTime, now)
                    .update();
        }
        authorizationCacheService.evictUsersByPermissionsAfterCommit(affectedPermissionIds);
    }

    @Override
    public boolean hasChildren(String permissionId) {
        if (!StringUtils.hasText(permissionId)) {
            return false;
        }
        return count(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getParentId, permissionId.trim())) > 0;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(AssignPermissionDto dto) {
        if (dto == null || !StringUtils.hasText(dto.getRoleId())) {
            throw new IllegalArgumentException("角色ID不能为空");
        }

        String roleId = dto.getRoleId().trim();
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || Integer.valueOf(1).equals(role.getIsDeleted())) {
            throw new IllegalArgumentException("角色不存在");
        }

        List<String> normalizedPermissionIds = normalizePermissionIds(resolvePermissionIds(dto));
        rolePermissionService.replaceRolePermissions(roleId, normalizedPermissionIds, currentOperatorId());
    }

    @Override
    public List<String> getPermissionCodesByUserId(String userId) {
        return StringUtils.hasText(userId)
                ? baseMapper.selectPermissionCodesByUserId(userId.trim())
                : Collections.emptyList();
    }


    @Override
    public List<SysPermission> getUserPageRoutes(String userId) {
        return StringUtils.hasText(userId)
                ? baseMapper.selectUserPageRoutes(userId.trim())
                : Collections.emptyList();
    }

    @Override
    public List<SysPermission> getUserHomeModules(String userId) {
        return StringUtils.hasText(userId)
                ? baseMapper.selectUserHomeModules(userId.trim())
                : Collections.emptyList();
    }

    @Override
    public List<RouterVo> getUserRouters(String userId) {
        return buildTree(getUserPageRoutes(userId)).stream()
                .map(this::toRouterVo)
                .collect(Collectors.toList());
    }

    private void validatePermission(PermissionSaveDto dto, String currentId) {
        if (dto == null) {
            throw new IllegalArgumentException("权限信息不能为空");
        }

        String name = resolvePermissionName(dto);
        String code = resolvePermissionCode(dto);
        Integer type = resolvePermissionType(dto);
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("权限名称不能为空");
        }
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("权限标识不能为空");
        }
        if (type == null || (type != TYPE_PAGE && type != TYPE_BUTTON)) {
            throw new IllegalArgumentException("权限类型只能为1页面或2按钮/接口");
        }
        validateStatus(dto.getStatus() == null ? 1 : dto.getStatus());

        if (StringUtils.hasText(currentId) && hasChildren(currentId) && type != TYPE_PAGE) {
            throw new IllegalArgumentException("存在子权限时不能修改为按钮权限");
        }

        if (type == TYPE_PAGE) {
            if (!StringUtils.hasText(resolveRoutePath(dto))) {
                throw new IllegalArgumentException("页面权限必须填写路由地址");
            }
            if (!StringUtils.hasText(resolveComponentPath(dto))) {
                throw new IllegalArgumentException("页面权限必须填写组件路径");
            }
            if (StringUtils.hasText(dto.getParentId()) && !ROOT_PARENT_ID.equals(dto.getParentId().trim())) {
                throw new IllegalArgumentException("页面权限必须位于根节点");
            }
        } else {
            String parentId = StringUtils.hasText(dto.getParentId()) ? dto.getParentId().trim() : ROOT_PARENT_ID;
            if (ROOT_PARENT_ID.equals(parentId)) {
                throw new IllegalArgumentException("按钮/接口权限必须选择所属页面");
            }
            if (StringUtils.hasText(currentId) && currentId.equals(parentId)) {
                throw new IllegalArgumentException("权限不能把自己设为父级");
            }
            SysPermission parent = getById(parentId);
            if (parent == null || !Integer.valueOf(TYPE_PAGE).equals(parent.getPermissionType())) {
                throw new IllegalArgumentException("按钮/接口权限的父级必须是页面权限");
            }
        }
    }

    private List<String> normalizePermissionIds(List<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> requestedIds = permissionIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requestedIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysPermission> permissions = listByIds(requestedIds);
        if (permissions.size() != requestedIds.size()) {
            throw new IllegalArgumentException("部分权限不存在或已删除");
        }

        Set<String> result = new LinkedHashSet<>();
        Set<String> parentIds = new LinkedHashSet<>();
        for (SysPermission permission : permissions) {
            if (!Integer.valueOf(1).equals(permission.getStatus())) {
                throw new IllegalArgumentException("不能分配已停用权限：" + permission.getPermissionName());
            }
            result.add(permission.getId());
            if (Integer.valueOf(TYPE_BUTTON).equals(permission.getPermissionType())
                    && StringUtils.hasText(permission.getParentId())
                    && !ROOT_PARENT_ID.equals(permission.getParentId())) {
                parentIds.add(permission.getParentId());
            }
        }

        if (!parentIds.isEmpty()) {
            List<SysPermission> parents = listByIds(parentIds);
            if (parents.size() != parentIds.size()) {
                throw new IllegalArgumentException("按钮权限所属页面不存在或已删除");
            }
            for (SysPermission parent : parents) {
                if (!Integer.valueOf(TYPE_PAGE).equals(parent.getPermissionType())
                        || !Integer.valueOf(1).equals(parent.getStatus())) {
                    throw new IllegalArgumentException("按钮权限所属页面已停用或类型不正确：" + parent.getPermissionName());
                }
                result.add(parent.getId());
            }
        }
        return new ArrayList<>(result);
    }

    private boolean existsPermissionCode(String permissionCode, String excludeId) {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermissionCode, permissionCode);
        if (StringUtils.hasText(excludeId)) {
            wrapper.ne(SysPermission::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    private void copyDtoToPermission(PermissionSaveDto dto, SysPermission permission, boolean create) {
        Integer type = resolvePermissionType(dto);
        permission.setParentId(type == TYPE_PAGE
                ? ROOT_PARENT_ID
                : dto.getParentId().trim());
        permission.setPermissionName(resolvePermissionName(dto).trim());
        permission.setPermissionCode(resolvePermissionCode(dto).trim());
        permission.setPermissionType(type);
        permission.setModuleCode(resolveModuleCode(dto));
        permission.setActionCode(resolveActionCode(dto));

        permission.setRoutePath(type == TYPE_PAGE ? trimToNull(resolveRoutePath(dto)) : null);
        permission.setRouteName(type == TYPE_PAGE ? trimToNull(dto.getRouteName()) : null);
        permission.setComponentPath(type == TYPE_PAGE ? trimToNull(resolveComponentPath(dto)) : null);
        permission.setRedirectPath(type == TYPE_PAGE ? trimToNull(dto.getRedirectPath()) : null);
        permission.setIcon(trimToNull(dto.getIcon()));
        permission.setVisible(dto.getVisible() == null ? 1 : dto.getVisible());
        permission.setKeepAlive(dto.getKeepAlive() == null ? 0 : dto.getKeepAlive());
        permission.setShowInHome(type == TYPE_PAGE && dto.getShowInHome() != null ? dto.getShowInHome() : 0);
        permission.setHomeTitle(type == TYPE_PAGE ? trimToNull(dto.getHomeTitle()) : null);
        permission.setHomeDescription(type == TYPE_PAGE ? trimToNull(dto.getHomeDescription()) : null);
        permission.setButtonKey(type == TYPE_BUTTON ? trimToNull(resolveButtonKey(dto)) : null);
        permission.setApiMethod(type == TYPE_BUTTON ? trimToNull(dto.getApiMethod()) : null);
        permission.setApiPath(type == TYPE_BUTTON ? trimToNull(dto.getApiPath()) : null);
        permission.setSortValue(resolveSortValue(dto));
        permission.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        permission.setRemark(trimToNull(dto.getRemark()));
        permission.setMetaJson(trimToNull(dto.getMetaJson()));
        if (create) {
            permission.setIsDeleted(0);
        }
    }

    private List<SysPermission> buildTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        permissions.forEach(item -> item.setChildren(new ArrayList<>()));
        Map<String, SysPermission> map = permissions.stream()
                .filter(item -> StringUtils.hasText(item.getId()))
                .collect(Collectors.toMap(SysPermission::getId, Function.identity(), (left, right) -> left));

        List<SysPermission> roots = new ArrayList<>();
        for (SysPermission permission : permissions) {
            String parentId = permission.getParentId();
            if (!StringUtils.hasText(parentId) || ROOT_PARENT_ID.equals(parentId) || !map.containsKey(parentId)) {
                roots.add(permission);
            } else {
                map.get(parentId).getChildren().add(permission);
            }
        }
        sortTree(roots);
        return roots;
    }

    private void sortTree(List<SysPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return;
        }
        permissions.sort(permissionComparator());
        permissions.forEach(item -> sortTree(item.getChildren()));
    }

    private Comparator<SysPermission> permissionComparator() {
        return Comparator.comparing(SysPermission::getSortValue, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SysPermission::getId, Comparator.nullsLast(String::compareTo));
    }

    private RouterVo toRouterVo(SysPermission permission) {
        RouterVo router = new RouterVo();
        router.setPath(permission.getRoutePath());
        router.setName(StringUtils.hasText(permission.getRouteName())
                ? permission.getRouteName()
                : permission.getPermissionName());
        router.setHidden(Integer.valueOf(0).equals(permission.getVisible()));
        router.setComponent(permission.getComponentPath());
        router.setAlwaysShow(permission.getChildren() != null && !permission.getChildren().isEmpty());
        router.setMeta(new MetaVo(permission.getPermissionName(), permission.getIcon(),
                Integer.valueOf(1).equals(permission.getKeepAlive())));
        if (permission.getChildren() != null) {
            router.setChildren(permission.getChildren().stream().map(this::toRouterVo).collect(Collectors.toList()));
        }
        return router;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new IllegalArgumentException("状态只能为0或1");
        }
    }

    private String resolvePermissionName(PermissionSaveDto dto) {
        return dto.getPermissionName();
    }

    private String resolvePermissionCode(PermissionSaveDto dto) {
        return dto.getPermissionCode();
    }

    private Integer resolvePermissionType(PermissionSaveDto dto) {
        return dto.getPermissionType();
    }

    private String resolveRoutePath(PermissionSaveDto dto) {
        return dto.getRoutePath();
    }

    private String resolveComponentPath(PermissionSaveDto dto) {
        return dto.getComponentPath();
    }

    private Integer resolveSortValue(PermissionSaveDto dto) {
        return dto.getSortValue() == null ? 0 : dto.getSortValue();
    }

    private String resolveButtonKey(PermissionSaveDto dto) {
        return StringUtils.hasText(dto.getButtonKey()) ? dto.getButtonKey() : resolvePermissionCode(dto);
    }

    private String resolveModuleCode(PermissionSaveDto dto) {
        if (StringUtils.hasText(dto.getModuleCode())) {
            return dto.getModuleCode().trim();
        }
        String code = resolvePermissionCode(dto);
        if (StringUtils.hasText(code) && code.contains(":")) {
            return code.substring(0, code.indexOf(':'));
        }
        return "system";
    }

    private String resolveActionCode(PermissionSaveDto dto) {
        if (StringUtils.hasText(dto.getActionCode())) {
            return dto.getActionCode().trim();
        }
        String code = resolvePermissionCode(dto);
        if (StringUtils.hasText(code) && code.contains(":")) {
            return code.substring(code.lastIndexOf(':') + 1);
        }
        return "page";
    }

    private List<String> resolvePermissionIds(AssignPermissionDto dto) {
        return dto.getPermissionIds();
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
