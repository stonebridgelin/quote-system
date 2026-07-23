package com.stonebridge.quotesystem.security.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Redis 中的用户授权快照，不包含密码或其他敏感资料。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationSnapshot {
    private String userId;
    private String username;
    private Integer status;
    private List<String> roles = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();
}
