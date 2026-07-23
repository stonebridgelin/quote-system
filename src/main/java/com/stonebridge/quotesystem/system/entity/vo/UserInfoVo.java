package com.stonebridge.quotesystem.system.entity.vo;

import com.stonebridge.quotesystem.system.entity.SysPermission;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * 登录后前端可基于 permissions 控制按钮，基于 routes 动态注册路由，基于 homeModules 渲染业务首页。
 */
@Data
public class UserInfoVo {
    private String id;
    private String username;
    private String nickname;
    private String realName;
    private String avatar;

    private List<String> roles;
    private List<String> permissions;

    /** 当前用户可访问页面路由，permissionType = 1。 */
    private List<SysPermission> routes;

    /** 当前用户业务首页模块，permissionType = 1 且 showInHome = 1。 */
    private List<SysPermission> homeModules;
}
