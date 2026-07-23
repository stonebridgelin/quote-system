-- quote.t_order_tracking 定义

CREATE TABLE `t_order_tracking` (
                                    `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
                                    `order_no` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单编号，CT-开头，唯一',
                                    `customer_order_no` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户订单编号',
                                    `customer_contact` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '客户对接人，格式：公司名-对接人',
                                    `shape_description` text COLLATE utf8mb4_general_ci COMMENT '器型说明，辅助文字描述',
                                    `review_form_image_url` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同评审表截图访问地址',
                                    `review_form_image_key` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '合同评审表截图OSS存储路径',
                                    `bottom_label` int DEFAULT '0' COMMENT '底标：0待确认，10无底标，20有底标',
                                    `bottom_label_status` int DEFAULT NULL COMMENT '底标当前状态，仅有底标时使用',
                                    `is_bottom_label_waiting` tinyint DEFAULT '0' COMMENT '底标是否等待回复：0否，1是',
                                    `sticker` int DEFAULT '0' COMMENT '不干胶：0待确认，10无，20客户提供，30工厂生产',
                                    `sticker_status` int DEFAULT NULL COMMENT '不干胶当前状态',
                                    `is_sticker_waiting` tinyint DEFAULT '0' COMMENT '不干胶是否等待回复：0否，1是',
                                    `printing` int DEFAULT '0' COMMENT '花纸：0待确认，10无花纸或白坯，20客户客供，30工厂提供',
                                    `printing_status` int DEFAULT NULL COMMENT '花纸当前状态，仅客户客供时使用',
                                    `printing_pattern_code` text COLLATE utf8mb4_general_ci COMMENT '花型代码，仅工厂提供花纸时填写',
                                    `is_printing_waiting` tinyint DEFAULT '0' COMMENT '花纸是否等待回复：0否，1是',
                                    `inner_box` int DEFAULT '0' COMMENT '黄盒：0待确认，10不印刷，20印刷',
                                    `inner_box_status` int DEFAULT NULL COMMENT '黄盒印刷当前状态',
                                    `is_inner_box_waiting` tinyint DEFAULT '0' COMMENT '黄盒是否等待回复：0否，1是',
                                    `color_box` int DEFAULT '0' COMMENT '彩盒：0待确认，10无，20客户提供，30工厂生产',
                                    `color_box_status` int DEFAULT NULL COMMENT '彩盒当前状态',
                                    `is_color_box_waiting` tinyint DEFAULT '0' COMMENT '彩盒是否等待回复：0否，1是',
                                    `carton_status` int DEFAULT '1100' COMMENT '外箱当前状态',
                                    `is_carton_waiting` tinyint DEFAULT '0' COMMENT '外箱是否等待回复：0否，1是',
                                    `separator_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '器型隔离物：珍珠棉、一层纸、两层纸',
                                    `packaging_requirement` text COLLATE utf8mb4_general_ci COMMENT '包装要求',
                                    `planned_production_date` date DEFAULT NULL COMMENT '计划生产日期',
                                    `planned_delivery_date` date NOT NULL COMMENT '计划交付日期',
                                    `return_sample` int DEFAULT '0' COMMENT '回签样：0无，10已寄出但未寄回，20已收到',
                                    `remark` text COLLATE utf8mb4_general_ci COMMENT '整体备注',
                                    `is_finished` tinyint DEFAULT '0' COMMENT '是否完成：0否，1是',
                                    `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0否，1是',
                                    `create_time` datetime NOT NULL COMMENT '创建时间',
                                    `create_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                    `update_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
                                    `anti_cut_board` int DEFAULT '0' COMMENT '防割板：0待确认，10无防割板，20一层防割板，30上下两层防割板',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_t_order_tracking_order_no` (`order_no`),
                                    KEY `idx_t_order_tracking_customer_order_no` (`customer_order_no`),
                                    KEY `idx_t_order_tracking_customer_contact` (`customer_contact`),
                                    KEY `idx_t_order_tracking_delivery_date` (`planned_delivery_date`),
                                    KEY `idx_t_order_tracking_production_date` (`planned_production_date`),
                                    KEY `idx_t_order_tracking_is_finished` (`is_finished`),
                                    KEY `idx_t_order_tracking_is_deleted` (`is_deleted`),
                                    KEY `idx_t_order_tracking_bottom_label` (`bottom_label`),
                                    KEY `idx_t_order_tracking_bottom_label_status` (`bottom_label_status`),
                                    KEY `idx_t_order_tracking_bottom_label_waiting` (`is_bottom_label_waiting`),
                                    KEY `idx_t_order_tracking_anti_cut_board` (`anti_cut_board`),
                                    KEY `idx_t_order_tracking_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单追踪管理主表';


-- quote.t_order_tracking_log 定义

CREATE TABLE `t_order_tracking_log` (
                                        `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
                                        `order_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单ID',
                                        `module_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块类型：STICKER、PRINTING、INNER_BOX、COLOR_BOX、CARTON',
                                        `before_type` int DEFAULT NULL COMMENT '修改前类型',
                                        `after_type` int DEFAULT NULL COMMENT '修改后类型',
                                        `before_status` int DEFAULT NULL COMMENT '修改前状态',
                                        `after_status` int DEFAULT NULL COMMENT '修改后状态',
                                        `before_text` text COLLATE utf8mb4_general_ci COMMENT '修改前文本内容，例如花型代码',
                                        `after_text` text COLLATE utf8mb4_general_ci COMMENT '修改后文本内容，例如花型代码',
                                        `remark_content` text COLLATE utf8mb4_general_ci COMMENT '本次更新备注',
                                        `create_time` datetime NOT NULL COMMENT '记录时间',
                                        `create_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '记录人',
                                        `sort_no` bigint DEFAULT '0' COMMENT '日志排序号：同一订单同一模块内递增',
                                        PRIMARY KEY (`id`),
                                        KEY `idx_t_order_tracking_log_order_id` (`order_id`),
                                        KEY `idx_t_order_tracking_log_module_type` (`module_type`),
                                        KEY `idx_t_order_tracking_log_create_time` (`create_time`),
                                        KEY `idx_tracking_log_order_module_sort` (`order_id`,`module_type`,`sort_no`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单追踪状态更新日志表';


-- quote.t_order_tracking_log_image 定义

CREATE TABLE `t_order_tracking_log_image` (
                                              `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
                                              `log_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '日志ID',
                                              `order_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单ID',
                                              `module_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块类型',
                                              `image_url` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '图片访问地址',
                                              `image_key` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '图片OSS存储路径',
                                              `original_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '原始文件名',
                                              `file_size` bigint DEFAULT NULL COMMENT '文件大小',
                                              `content_type` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '文件类型',
                                              `sort_no` int DEFAULT '0' COMMENT '排序',
                                              `create_time` datetime NOT NULL COMMENT '创建时间',
                                              `create_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                              PRIMARY KEY (`id`),
                                              KEY `idx_t_order_tracking_log_image_log_id` (`log_id`),
                                              KEY `idx_t_order_tracking_log_image_order_id` (`order_id`),
                                              KEY `idx_t_order_tracking_log_image_module_type` (`module_type`),
                                              KEY `idx_tracking_log_image_log_sort` (`log_id`,`sort_no`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单追踪日志图片表';


-- quote.t_order_tracking_option 定义

CREATE TABLE `t_order_tracking_option` (
                                           `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
                                           `module_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块类型：STICKER、PRINTING、INNER_BOX、COLOR_BOX、CARTON',
                                           `option_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置类型：TYPE类型配置，STATUS状态配置',
                                           `parent_value` int DEFAULT NULL COMMENT '父级类型值，例如客户提供20、工厂生产30；状态配置时使用',
                                           `option_value` int NOT NULL COMMENT '配置值，例如10、20、2100、2200',
                                           `option_label` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示名称',
                                           `sort_no` int DEFAULT '0' COMMENT '排序',
                                           `tag_type` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端标签类型：info、warning、success、danger、primary',
                                           `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '备注说明',
                                           `is_enabled` tinyint DEFAULT '1' COMMENT '是否启用：0否，1是',
                                           `is_deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0否，1是',
                                           `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                                           `create_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '创建人',
                                           `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                                           `update_by` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '更新人',
                                           PRIMARY KEY (`id`),
                                           KEY `idx_tracking_option_module_type` (`module_type`),
                                           KEY `idx_tracking_option_option_type` (`option_type`),
                                           KEY `idx_tracking_option_parent_value` (`parent_value`),
                                           KEY `idx_tracking_option_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='订单追踪配置选项表';


-- quote.t_pack_spec 定义

CREATE TABLE `t_pack_spec` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                               `spec_code` varchar(50) NOT NULL COMMENT '关联器型规格代码 (对应 t_shape_spec.spec_code)',
                               `pack_model` varchar(100) NOT NULL COMMENT '原始包装型号 (如 LHKW50-6-8外箱)',
                               `pcs_per_box` int NOT NULL COMMENT '每黄盒数量 (PCS/SET)',
                               `boxes_per_carton` int NOT NULL COMMENT '每箱黄盒数 (SETS/CTN)',
                               `outer_length` decimal(8,2) DEFAULT NULL COMMENT '外箱长 (cm)',
                               `outer_width` decimal(8,2) DEFAULT NULL COMMENT '外箱宽 (cm)',
                               `outer_height` decimal(8,2) DEFAULT NULL COMMENT '外箱高 (cm)',
                               `inner_length` decimal(8,2) DEFAULT NULL COMMENT '黄盒长 (cm)',
                               `inner_width` decimal(8,2) DEFAULT NULL COMMENT '黄盒宽 (cm)',
                               `inner_height` decimal(8,2) DEFAULT NULL COMMENT '黄盒高 (cm)',
                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               KEY `idx_spec_code` (`spec_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2281 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='包装规格数据表';


-- quote.t_quote_detail 定义

CREATE TABLE `t_quote_detail` (
                                  `id` varchar(32) NOT NULL,
                                  `order_id` varchar(32) NOT NULL,
                                  `item_index` int NOT NULL COMMENT '显示序号',
                                  `customer_item_no` varchar(50) DEFAULT NULL COMMENT '客户货号',
                                  `shape` varchar(100) DEFAULT NULL COMMENT '器型',
                                  `pcs_per_set` int DEFAULT NULL COMMENT '只/套',
                                  `sets_per_ctn` int DEFAULT NULL COMMENT '套/箱',
                                  `qty` int DEFAULT NULL COMMENT '数量(总只数)',
                                  `sets` int DEFAULT NULL COMMENT '套数(自动计算)',
                                  `ctns` int DEFAULT NULL COMMENT '箱数(自动计算)',
                                  `decal` varchar(100) DEFAULT NULL COMMENT '花型',
                                  `package_req` varchar(200) DEFAULT NULL COMMENT '包材要求',
                                  `combo_mark` varchar(50) DEFAULT NULL COMMENT '组合标识(填入相同标识视为同一组合产品)',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';


-- quote.t_quote_main 定义

CREATE TABLE `t_quote_main` (
                                `id` varchar(32) NOT NULL COMMENT 'UUID',
                                `order_no` varchar(50) NOT NULL COMMENT '订单号(CT-开头)',
                                `po_no` varchar(100) DEFAULT NULL COMMENT '客户订单号',
                                `contact` varchar(100) DEFAULT NULL COMMENT '客户对接人',
                                `container_qty` decimal(10,2) DEFAULT NULL COMMENT '柜量数字',
                                `container_type` varchar(20) DEFAULT NULL COMMENT '柜型(GP/40GP/40HQ)',
                                `total_qty` int DEFAULT '0' COMMENT '总数量',
                                `total_sets` int DEFAULT '0' COMMENT '总套数',
                                `total_ctns` int DEFAULT '0' COMMENT '总箱数',
                                `delivery_date` date DEFAULT NULL COMMENT '约定交期',
                                `decal_status` tinyint DEFAULT '0' COMMENT '花纸: 0无, 1待确认(红), 2已确认(绿)',
                                `logo_status` tinyint DEFAULT '0' COMMENT '底标: 0无, 1待确认(红), 2已确认(绿)',
                                `package_status` tinyint DEFAULT '1' COMMENT '包材: 1待确认(红), 2已确认(绿)',
                                `inspection_date` date DEFAULT NULL COMMENT '验货日期',
                                `pre_prod_sample` text COMMENT '产前样',
                                `signed_sample` text COMMENT '回签样',
                                `test_sample` text COMMENT '测试样',
                                `bulk_sample` text COMMENT '大货样',
                                `remark` text COMMENT '备注信息',
                                `status` tinyint DEFAULT '0' COMMENT '状态: 0新建, 1生产中, 2完成',
                                `creator` varchar(50) DEFAULT NULL COMMENT '创建人',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                `del_flag` tinyint DEFAULT '0' COMMENT '逻辑删除: 0正常, 1删除',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单追踪主表';


-- quote.t_sample_image 定义

CREATE TABLE `t_sample_image` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `sample_id` bigint NOT NULL COMMENT '关联的样品单ID',
                                  `base64_data` longtext NOT NULL COMMENT '图片的Base64编码数据',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='样品图片附件表';


-- quote.t_sample_tracking 定义

CREATE TABLE `t_sample_tracking` (
                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                     `customer_info` varchar(150) NOT NULL COMMENT '客户信息(公司名-对接人)',
                                     `plan_date` date NOT NULL COMMENT '计划完成日期',
                                     `status` varchar(20) NOT NULL DEFAULT 'MAKING' COMMENT '状态: MAKING(制作中), SHIPPED(已打包寄出), ENDED(结束)',
                                     `tracking_no` varchar(100) DEFAULT NULL COMMENT '快递单号',
                                     `remarks` text COMMENT '备注信息',
                                     `creator` varchar(50) DEFAULT NULL COMMENT '创建人账号',
                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
                                     `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最新更新时间',
                                     `del_flag` tinyint DEFAULT '0' COMMENT '逻辑删除: 0正常, 1删除',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='样品追踪主表';


-- quote.t_shape 定义

CREATE TABLE `t_shape` (
                           `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                           `shape_code` varchar(50) NOT NULL COMMENT '器型代号 (如 FLXRKP)',
                           `shape_name` varchar(100) NOT NULL COMMENT '器型中文名称',
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `image_data` mediumblob,
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_shape_code` (`shape_code`)
) ENGINE=InnoDB AUTO_INCREMENT=553 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='器型基础表';


-- quote.t_shape_spec 定义

CREATE TABLE `t_shape_spec` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `shape_code` varchar(50) NOT NULL COMMENT '关联器型代号 (对应 t_shape.shape_code)',
                                `spec_code` varchar(50) NOT NULL COMMENT '器型规格代码 (如 FLXRKP85)',
                                `size` varchar(50) DEFAULT NULL COMMENT '解析出的尺寸 (如 8.5 英寸)',
                                `weight` decimal(10,2) NOT NULL COMMENT '重量 (单位: g)',
                                `description` varchar(255) DEFAULT NULL COMMENT '产品描述 (DESCRIPTION)',
                                `ton_price` int NOT NULL COMMENT '价格 (单位: 元/吨，如 7000)',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_spec_code` (`spec_code`),
                                KEY `idx_shape_code` (`shape_code`)
) ENGINE=InnoDB AUTO_INCREMENT=696 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='器型规格数据表';


-- quote.t_sys_config 定义

CREATE TABLE `t_sys_config` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                `config_key` varchar(100) NOT NULL COMMENT '配置键 (如 USD_EXCHANGE_RATE)',
                                `config_value` varchar(255) NOT NULL COMMENT '配置值 (如 6.8)',
                                `remark` varchar(255) DEFAULT NULL COMMENT '备注',
                                PRIMARY KEY (`id`),
                                UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统配置表';


-- quote.t_user 定义

CREATE TABLE `t_user` (
                          `id` bigint NOT NULL AUTO_INCREMENT,
                          `username` varchar(50) NOT NULL COMMENT '登录账号',
                          `password` varchar(100) NOT NULL COMMENT 'BCrypt盐值加密密码',
                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
                          `name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
                          `role` varchar(20) DEFAULT 'USER' COMMENT '角色标识(如: ADMIN, USER)',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;