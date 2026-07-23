package com.stonebridge.quotesystem.system.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PermissionSaveDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String permissionName;
    private String permissionCode;
    private Integer permissionType;
    private String moduleCode;
    private String actionCode;
    private String routePath;
    private String routeName;
    private String componentPath;
    private String redirectPath;
    private String icon;
    private Integer visible;
    private Integer keepAlive;
    private Integer showInHome;
    private String homeTitle;
    private String homeDescription;
    private String buttonKey;
    private String apiMethod;
    private String apiPath;
    private Integer sortValue;
    private Integer status;
    private String remark;
    private String metaJson;
}
