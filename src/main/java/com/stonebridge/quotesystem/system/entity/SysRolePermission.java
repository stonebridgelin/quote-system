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
 * 角色权限关联表。
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID，32位UUID。 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 角色ID。 */
    @TableField("role_id")
    private String roleId;

    /** 权限ID。 */
    @TableField("permission_id")
    private String permissionId;

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
