package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("t_shape") // 指定对应的数据库表名
public class Shape {

    @TableId(type = IdType.AUTO) // 指定主键自增
    private Long id;

    private String shapeCode; // 对应数据库字段 shape_code

    private String shapeName; // 对应数据库字段 shape_name

    private Date createTime;

    private Date updateTime;

    private byte[] imageData;
}