package com.stonebridge.quotesystem.system.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RoleSaveDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String roleName;
    private String roleCode;
    private String description;
    private Integer sortValue;
    private Integer status;
    private String remark;
    private List<String> permissionIds;
}
