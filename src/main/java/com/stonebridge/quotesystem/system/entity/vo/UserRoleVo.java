package com.stonebridge.quotesystem.system.entity.vo;

import com.stonebridge.quotesystem.system.entity.SysRole;
import lombok.Data;

import java.util.List;

@Data
public class UserRoleVo {
    private String userId;
    private List<SysRole> allRoles;
    private List<String> assignedRoleIds;
}
