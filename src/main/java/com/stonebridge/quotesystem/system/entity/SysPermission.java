package com.stonebridge.quotesystem.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统权限表。
 *
 * permissionType = 1：页面权限；
 * permissionType = 2：按钮/接口权限。
 */
@Data
@TableName("sys_permissions")
public class SysPermission implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 权限ID，32位UUID。 */
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;

    /** 父级权限ID；页面为0，按钮挂在页面权限下。 */
    @TableField("parent_id")
    private String parentId;

    /** 权限名称，例如：报价管理、新增报价。 */
    @TableField("permission_name")
    private String permissionName;

    /** 权限标识，例如：quote:page、quote:create。 */
    @TableField("permission_code")
    private String permissionCode;

    /** 权限类型：1页面，2按钮/接口。 */
    @TableField("permission_type")
    private Integer permissionType;

    /** 模块编码，例如：quote、basic、sample、order-tracking、system。 */
    @TableField("module_code")
    private String moduleCode;

    /** 动作编码，例如：page、create、update、delete、export。 */
    @TableField("action_code")
    private String actionCode;

    /** 前端路由路径；页面权限使用，例如：/quote-maker。 */
    @TableField("route_path")
    private String routePath;

    /** 前端路由名称；页面权限使用，例如：QuoteMaker。 */
    @TableField("route_name")
    private String routeName;

    /** 前端组件路径；页面权限使用，例如：QuoteMaker 或 business/QuoteMaker。 */
    @TableField("component_path")
    private String componentPath;

    /** 重定向路径；可为空。 */
    @TableField("redirect_path")
    private String redirectPath;

    /** 图标。 */
    @TableField("icon")
    private String icon;

    /** 是否在菜单/页面入口显示：1显示，0隐藏。 */
    @TableField("visible")
    private Integer visible;

    /** 页面是否缓存：1缓存，0不缓存。 */
    @TableField("keep_alive")
    private Integer keepAlive;

    /** 是否在业务首页显示：1显示，0不显示。 */
    @TableField("show_in_home")
    private Integer showInHome;

    /** 首页模块标题。 */
    @TableField("home_title")
    private String homeTitle;

    /** 首页模块描述。 */
    @TableField("home_description")
    private String homeDescription;

    /** 前端按钮标识；按钮权限使用，通常与 permissionCode 一致。 */
    @TableField("button_key")
    private String buttonKey;

    /** 后端接口请求方式，例如：GET、POST、PUT、DELETE。 */
    @TableField("api_method")
    private String apiMethod;

    /** 后端接口路径，例如：/api/quote/export。 */
    @TableField("api_path")
    private String apiPath;

    /** 排序值，越小越靠前。 */
    @TableField("sort_value")
    private Integer sortValue;

    /** 状态：1正常，0停用。 */
    @TableField("status")
    private Integer status;

    /** 备注。 */
    @TableField("remark")
    private String remark;

    /** 前端路由扩展配置JSON，例如props、keepAlive等。 */
    @TableField("meta_json")
    private String metaJson;

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

    /** 子权限，用于树形展示，不映射数据库字段。 */
    @TableField(exist = false)
    private List<SysPermission> children;

    /**
     * 角色授权准备接口使用：当前角色是否直接拥有该权限。
     * 仅用于接口展示，不映射数据库字段。
     */
    @TableField(exist = false)
    private Boolean selected;
}
