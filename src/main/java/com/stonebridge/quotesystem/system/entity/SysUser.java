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
 * 系统用户表。
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户ID，32位UUID。 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 登录账号，系统内唯一。 */
    @TableField("username")
    private String username;

    /** BCrypt 加密后的密码。 */
    @TableField("password")
    private String password;

    /** 昵称。 */
    @TableField("nickname")
    private String nickname;

    /** 真实姓名。 */
    @TableField("real_name")
    private String realName;

    /** 性别：0男，1女，2未知。 */
    @TableField("gender")
    private Integer gender;

    /** 手机号。 */
    @TableField("phone")
    private String phone;

    /** 邮箱。 */
    @TableField("email")
    private String email;

    /** 头像地址。 */
    @TableField("avatar")
    private String avatar;

    /** 用户类型：0管理员，1普通用户。 */
    @TableField("user_type")
    private Integer userType;

    /** 状态：1正常，0停用。 */
    @TableField("status")
    private Integer status;

    /** 最后登录时间。 */
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    /** 最后登录IP。 */
    @TableField("last_login_ip")
    private String lastLoginIp;

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
