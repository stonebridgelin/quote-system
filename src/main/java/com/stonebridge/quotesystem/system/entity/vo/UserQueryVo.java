package com.stonebridge.quotesystem.system.entity.vo;

import lombok.Data;

@Data
public class UserQueryVo {
    private String keyword;
    private String createTimeBegin;
    private String createTimeEnd;
    private Integer status;
}
