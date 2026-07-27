package com.stonebridge.quotesystem.business.entity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderTrackingStatusOptionUpdateDTO {

    /**
     * 状态码。
     */
    @NotNull(message = "状态码不能为空")
    @Positive(message = "状态码必须大于0")
    private Integer optionValue;

    /**
     * 前端展示名称。
     */
    @NotBlank(message = "状态名称不能为空")
    @Size(max = 200, message = "状态名称不能超过200个字符")
    private String optionLabel;

    /**
     * 同一模块和类型下的显示顺序。
     */
    @NotNull(message = "排序值不能为空")
    private Integer sortNo;

    /**
     * Element Plus 标签样式。
     */
    @NotBlank(message = "标签样式不能为空")
    @Pattern(
            regexp = "info|warning|success|danger|primary",
            message = "标签样式不正确")
    private String tagType;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
