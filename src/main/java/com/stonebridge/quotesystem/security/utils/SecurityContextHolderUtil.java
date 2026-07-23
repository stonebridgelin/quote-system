package com.stonebridge.quotesystem.security.utils;

import com.stonebridge.quotesystem.security.entity.SecurityUser;

/** 兼容旧项目工具类命名。 */
public final class SecurityContextHolderUtil {
    private SecurityContextHolderUtil() {
    }

    public static SecurityUser getCurrentUser() {
        return SecurityUtil.getCurrentUser();
    }

    public static String getUserId() {
        return SecurityUtil.getCurrentUserId();
    }

    public static String getUsername() {
        return SecurityUtil.getCurrentUsername();
    }
}
