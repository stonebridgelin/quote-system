package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_shape_spec")
public class ShapeSpec {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shapeCode;
    private String specCode;
    private String size;
    private BigDecimal weight;
    private String description;
    private Integer tonPrice; // 吨价是整数
    private Date createTime;
    private Date updateTime;
}