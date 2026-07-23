package com.stonebridge.quotesystem.system.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UserSaveDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String id;
    private String username;
    private String password;
    private String nickname;
    private String realName;
    private Integer gender;
    private String phone;
    private String email;
    private String avatar;
    private Integer userType;
    private Integer status;
    private String remark;
    private List<String> roleIds;
}
