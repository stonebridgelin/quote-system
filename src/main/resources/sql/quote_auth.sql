-- quote_auth.sys_permissions 定义

CREATE TABLE `sys_permissions` (
                                   `id` char(32) NOT NULL COMMENT '权限ID，32位UUID，由后端生成',
                                   `parent_id` char(32) NOT NULL DEFAULT '0' COMMENT '父级权限ID；页面为0，按钮挂在页面权限下',
                                   `permission_name` varchar(80) NOT NULL COMMENT '权限名称，例如：报价管理、新增报价',
                                   `permission_code` varchar(128) NOT NULL COMMENT '权限标识，例如：quote:page、quote:create',
                                   `permission_type` tinyint NOT NULL COMMENT '权限类型：1页面，2按钮/接口',
                                   `module_code` varchar(64) NOT NULL COMMENT '模块编码，例如：quote、basic、sample、order-tracking、system',
                                   `action_code` varchar(64) NOT NULL COMMENT '动作编码，例如：page、create、update、delete、export',
                                   `route_path` varchar(255) DEFAULT NULL COMMENT '前端路由路径；页面权限使用，例如：/quote-maker',
                                   `route_name` varchar(128) DEFAULT NULL COMMENT '前端路由名称；页面权限使用，例如：QuoteMaker',
                                   `component_path` varchar(255) DEFAULT NULL COMMENT '前端组件路径；页面权限使用，例如：QuoteMaker 或 business/QuoteMaker',
                                   `redirect_path` varchar(255) DEFAULT NULL COMMENT '重定向路径；可为空',
                                   `icon` varchar(100) DEFAULT NULL COMMENT '图标',
                                   `visible` tinyint NOT NULL DEFAULT '1' COMMENT '是否在菜单/页面入口显示：1显示，0隐藏',
                                   `keep_alive` tinyint NOT NULL DEFAULT '0' COMMENT '页面是否缓存：1缓存，0不缓存',
                                   `show_in_home` tinyint NOT NULL DEFAULT '0' COMMENT '是否在业务首页显示：1显示，0不显示',
                                   `home_title` varchar(80) DEFAULT NULL COMMENT '首页模块标题',
                                   `home_description` varchar(255) DEFAULT NULL COMMENT '首页模块描述',
                                   `button_key` varchar(128) DEFAULT NULL COMMENT '前端按钮标识；按钮权限使用，通常与permission_code一致',
                                   `api_method` varchar(20) DEFAULT NULL COMMENT '后端接口请求方式，例如：GET、POST、PUT、DELETE',
                                   `api_path` varchar(255) DEFAULT NULL COMMENT '后端接口路径，例如：/api/quote/export',
                                   `sort_value` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
                                   `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常，0停用',
                                   `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                                   `meta_json` json DEFAULT NULL COMMENT '前端路由扩展配置JSON，例如props、keepAlive等',
                                   `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
                                   `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `create_by` char(32) DEFAULT NULL COMMENT '创建人ID',
                                   `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `update_by` char(32) DEFAULT NULL COMMENT '更新人ID',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_sys_permissions_code` (`permission_code`),
                                   KEY `idx_sys_permissions_parent` (`parent_id`),
                                   KEY `idx_sys_permissions_type` (`permission_type`),
                                   KEY `idx_sys_permissions_module` (`module_code`),
                                   KEY `idx_sys_permissions_status_deleted` (`status`,`is_deleted`),
                                   KEY `idx_sys_permissions_home` (`show_in_home`,`status`,`is_deleted`),
                                   KEY `idx_sys_permissions_sort` (`sort_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限表：页面权限和按钮接口权限';


-- quote_auth.sys_role 定义

CREATE TABLE `sys_role` (
                            `id` char(32) NOT NULL COMMENT '角色ID，32位UUID，由后端生成',
                            `role_name` varchar(64) NOT NULL COMMENT '角色名称',
                            `role_code` varchar(64) NOT NULL COMMENT '角色编码，例如：admin、sales、manager',
                            `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
                            `sort_value` int NOT NULL DEFAULT '0' COMMENT '排序值，越小越靠前',
                            `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常，0停用',
                            `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                            `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `create_by` char(32) DEFAULT NULL COMMENT '创建人ID',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `update_by` char(32) DEFAULT NULL COMMENT '更新人ID',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_sys_role_code` (`role_code`),
                            KEY `idx_sys_role_status_deleted` (`status`,`is_deleted`),
                            KEY `idx_sys_role_sort` (`sort_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';


-- quote_auth.sys_user 定义

CREATE TABLE `sys_user` (
                            `id` char(32) NOT NULL COMMENT '用户ID，32位UUID，由后端生成',
                            `username` varchar(64) NOT NULL COMMENT '登录账号',
                            `password` varchar(128) NOT NULL COMMENT 'BCrypt加密密码',
                            `nickname` varchar(64) DEFAULT '' COMMENT '昵称',
                            `real_name` varchar(64) DEFAULT '' COMMENT '真实姓名',
                            `gender` tinyint NOT NULL DEFAULT '2' COMMENT '性别：0男，1女，2未知',
                            `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
                            `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
                            `avatar` varchar(255) DEFAULT NULL COMMENT '头像地址',
                            `user_type` tinyint NOT NULL DEFAULT '1' COMMENT '用户类型：0管理员，1普通用户',
                            `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1正常，0停用',
                            `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
                            `last_login_ip` varchar(64) DEFAULT NULL COMMENT '最后登录IP',
                            `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                            `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
                            `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `create_by` char(32) DEFAULT NULL COMMENT '创建人ID',
                            `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `update_by` char(32) DEFAULT NULL COMMENT '更新人ID',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_sys_user_username` (`username`),
                            KEY `idx_sys_user_status_deleted` (`status`,`is_deleted`),
                            KEY `idx_sys_user_type` (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';


-- quote_auth.sys_role_permission 定义

CREATE TABLE `sys_role_permission` (
                                       `id` char(32) NOT NULL COMMENT '主键ID，32位UUID，由后端生成',
                                       `role_id` char(32) NOT NULL COMMENT '角色ID',
                                       `permission_id` char(32) NOT NULL COMMENT '权限ID',
                                       `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `create_by` char(32) DEFAULT NULL COMMENT '创建人ID',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `update_by` char(32) DEFAULT NULL COMMENT '更新人ID',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_sys_role_permission` (`role_id`,`permission_id`),
                                       KEY `idx_sys_role_permission_role` (`role_id`),
                                       KEY `idx_sys_role_permission_permission` (`permission_id`),
                                       KEY `idx_sys_role_permission_deleted` (`is_deleted`),
                                       CONSTRAINT `fk_sys_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `sys_permissions` (`id`) ON DELETE CASCADE,
                                       CONSTRAINT `fk_sys_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';


-- quote_auth.sys_user_role 定义

CREATE TABLE `sys_user_role` (
                                 `id` char(32) NOT NULL COMMENT '主键ID，32位UUID，由后端生成',
                                 `user_id` char(32) NOT NULL COMMENT '用户ID',
                                 `role_id` char(32) NOT NULL COMMENT '角色ID',
                                 `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0未删除，1已删除',
                                 `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `create_by` char(32) DEFAULT NULL COMMENT '创建人ID',
                                 `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `update_by` char(32) DEFAULT NULL COMMENT '更新人ID',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_sys_user_role` (`user_id`,`role_id`),
                                 KEY `idx_sys_user_role_user` (`user_id`),
                                 KEY `idx_sys_user_role_role` (`role_id`),
                                 KEY `idx_sys_user_role_deleted` (`is_deleted`),
                                 CONSTRAINT `fk_sys_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`) ON DELETE CASCADE,
                                 CONSTRAINT `fk_sys_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';