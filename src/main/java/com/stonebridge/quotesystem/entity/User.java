package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    // ★ 新增：姓名和角色字段
    private String name;
    private String role;
    private String username;
    private String password;
    private LocalDateTime createTime;
}
