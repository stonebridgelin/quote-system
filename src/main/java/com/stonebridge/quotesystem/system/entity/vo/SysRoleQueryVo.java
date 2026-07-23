package com.stonebridge.quotesystem.system.entity.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class SysRoleQueryVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String keyword;
    private Integer status;
}
