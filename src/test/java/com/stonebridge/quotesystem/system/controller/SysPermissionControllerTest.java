package com.stonebridge.quotesystem.system.controller;

import com.stonebridge.quotesystem.common.Result;
import com.stonebridge.quotesystem.system.entity.SysPermission;
import com.stonebridge.quotesystem.system.mapper.SysRolePermissionMapper;
import com.stonebridge.quotesystem.system.service.SysPermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysPermissionControllerTest {

    @Mock
    private SysPermissionService sysPermissionService;

    @Mock
    private SysRolePermissionMapper sysRolePermissionMapper;

    private SysPermissionController controller;

    @BeforeEach
    void setUp() {
        controller = new SysPermissionController(sysPermissionService, sysRolePermissionMapper);
    }

    @Test
    void toAssignShouldReturnFullTreeAndMarkAssignedNodes() {
        SysPermission query = permission("query", List.of());
        SysPermission create = permission("create", List.of());
        SysPermission page = permission("page", List.of(query, create));
        when(sysPermissionService.findPermissionTree()).thenReturn(List.of(page));
        when(sysRolePermissionMapper.selectPermissionIdsByRoleId("role-1"))
                .thenReturn(List.of("page", "query"));

        Result<Map<String, Object>> result = controller.toAssign("role-1");
        Map<String, Object> data = result.getData();

        assertEquals(List.of("page", "query"), data.get("assignedPermissionIds"));
        assertEquals(List.of("query"), data.get("checkedPermissionIds"));
        assertTrue(page.getSelected());
        assertTrue(query.getSelected());
        assertFalse(create.getSelected());
        assertEquals(List.of(query, create), page.getChildren());
    }

    private SysPermission permission(String id, List<SysPermission> children) {
        SysPermission permission = new SysPermission();
        permission.setId(id);
        permission.setChildren(children);
        return permission;
    }
}
