package com.stonebridge.quotesystem.system.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class AssignPermissionDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String roleId;
    private List<String> permissionIds;
}
