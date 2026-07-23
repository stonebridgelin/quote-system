// SampleImage.java
package com.stonebridge.quotesystem.business.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("t_sample_image")
public class SampleImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sampleId;
    private String base64Data;
}