package com.stonebridge.quotesystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_pack_spec")
public class PackSpec {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String specCode;
    private String packModel;
    private Integer pcsPerBox;
    private Integer boxesPerCarton;
    private BigDecimal outerLength;
    private BigDecimal outerWidth;
    private BigDecimal outerHeight;
    private BigDecimal innerLength;
    private BigDecimal innerWidth;
    private BigDecimal innerHeight;
    private Date createTime;
    private Date updateTime;
}