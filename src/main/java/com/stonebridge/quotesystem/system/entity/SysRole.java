package com.stonebridge.quotesystem.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色表。
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 角色ID，32位UUID。 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 角色名称。 */
    @TableField("role_name")
    private String roleName;

    /** 角色编码，例如：admin、sales、manager。 */
    @TableField("role_code")
    private String roleCode;

    /** 角色描述。 */
    @TableField("description")
    private String description;

    /** 排序值，越小越靠前。 */
    @TableField("sort_value")
    private Integer sortValue;

    /** 状态：1正常，0停用。 */
    @TableField("status")
    private Integer status;

    /** 备注。 */
    @TableField("remark")
    private String remark;

    /** 逻辑删除：0未删除，1已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间。 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 创建人ID。 */
    @TableField("create_by")
    private String createBy;

    /** 更新时间。 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 更新人ID。 */
    @TableField("update_by")
    private String updateBy;
}
