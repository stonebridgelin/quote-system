package com.stonebridge.quotesystem.entity.dto;

import lombok.Data;

@Data
public class RegisterDTO {
    private String username;
    private String password;

    // ★ 新增：接收前端传来的姓名
    private String name;
}
