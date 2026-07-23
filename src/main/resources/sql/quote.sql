-- quote.t_order_record_image 定义

CREATE TABLE `t_order_record_image` (
                                        `id` varchar(32) NOT NULL,
                                        `record_id` varchar(32) NOT NULL,
                                        `base64_data` longtext NOT NULL COMMENT '图片Base64',
                                        `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                        PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


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
                                  `id` varchar(32) NOT NULL COMMENT '主键ID(UUID)',
                                  `order_id` varchar(32) DEFAULT NULL COMMENT '兼容运行库原有的主外键列',
                                  `quote_no` varchar(50) DEFAULT NULL COMMENT '报价单号',
                                  `item_index` int DEFAULT NULL COMMENT '序号',
                                  `line_type` varchar(20) DEFAULT NULL COMMENT '类型: SINGLE单品/SET套装',
                                  `set_group_id` varchar(32) DEFAULT NULL COMMENT '套装ID',
                                  `set_name` varchar(100) DEFAULT NULL COMMENT '套装名称',
                                  `set_unit_price` decimal(10,2) DEFAULT NULL COMMENT '套装单价',
                                  `spec_code` varchar(100) DEFAULT NULL COMMENT '器型规格代码',
                                  `design` varchar(100) DEFAULT NULL COMMENT '花面设计',
                                  `pcs_per_set` int DEFAULT NULL COMMENT 'PCS/SET',
                                  `sets_per_ctn` int DEFAULT NULL COMMENT 'SETS/CTN',
                                  `pcs` int DEFAULT NULL COMMENT '一箱总只数',
                                  `ttl_pcs` int DEFAULT NULL COMMENT '总件数',
                                  `ctns` int DEFAULT NULL COMMENT '箱数',
                                  `cbm_ctn` decimal(10,3) DEFAULT NULL COMMENT '单箱体积',
                                  `gw_ctn` decimal(10,2) DEFAULT NULL COMMENT '单箱毛重',
                                  `nw_ctn` decimal(10,2) DEFAULT NULL COMMENT '单箱净重',
                                  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
                                  `weight` decimal(10,2) DEFAULT NULL COMMENT '净重',
                                  `dimension` varchar(100) DEFAULT NULL COMMENT '尺寸展示',
                                  `original_price` decimal(10,2) DEFAULT NULL COMMENT '吨价快照',
                                  `carton_weight` decimal(10,2) DEFAULT NULL COMMENT '外箱重量',
                                  `extra_price` decimal(10,2) DEFAULT NULL COMMENT '额外价格',
                                  `remarks` varchar(255) DEFAULT NULL COMMENT '备注信息',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价单明细表';


-- quote.t_quote_main 定义

CREATE TABLE `t_quote_main` (
                                `id` varchar(32) NOT NULL COMMENT '主键ID(UUID)',
                                `order_no` varchar(50) DEFAULT NULL COMMENT '运行库保留的旧订单号列',
                                `quote_no` varchar(50) DEFAULT NULL COMMENT '报价单号',
                                `currency` varchar(10) DEFAULT NULL COMMENT '币种',
                                `exchange_rate` decimal(10,4) DEFAULT NULL COMMENT '汇率',
                                `remark` varchar(500) DEFAULT NULL COMMENT '备注',
                                `creator` varchar(50) DEFAULT NULL COMMENT '创建人',
                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价主表';


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



CREATE TABLE `t_quote_set_item` (
                                    `id` varchar(32) NOT NULL COMMENT '套装组件ID，32位UUID',
                                    `quote_detail_id` varchar(32) NOT NULL COMMENT '所属SET报价明细ID',
                                    `sort_no` int NOT NULL COMMENT '组件在套装内的显示顺序',
                                    `spec_code` varchar(50) NOT NULL COMMENT '组件产品规格编码',
                                    `design` varchar(100) DEFAULT NULL COMMENT '组件设计/花面',
                                    `weight` decimal(12,3) NOT NULL COMMENT '组件单件重量（克）',
                                    `original_price` decimal(18,4) NOT NULL COMMENT '组件原始吨价',
                                    `qty_per_set` int NOT NULL COMMENT '每套包含该组件的件数',
                                    `component_unit_price` decimal(18,6) NOT NULL COMMENT '组件单件价格快照',
                                    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_quote_set_item_sort` (`quote_detail_id`,`sort_no`),
                                    KEY `idx_quote_set_item_spec_code` (`spec_code`),
                                    CONSTRAINT `fk_quote_set_item_detail`
                                        FOREIGN KEY (`quote_detail_id`) REFERENCES `t_quote_detail` (`id`) ON DELETE CASCADE,
                                    CONSTRAINT `chk_quote_set_item_qty`
                                        CHECK (`qty_per_set` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报价套装组件表';


-- quote.t_customer 定义

CREATE TABLE `t_customer` (
                              `id` varchar(32) NOT NULL COMMENT '客户ID，32位UUID',
                              `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户基础表';


-- quote.t_signed_order_main 定义

CREATE TABLE `t_signed_order_main` (
                                       `id` varchar(32) NOT NULL COMMENT '签单ID，32位UUID',
                                       `order_no` varchar(100) NOT NULL COMMENT '订单编号',
                                       `customer_id` varchar(32) NOT NULL COMMENT '客户ID',
                                       `customer_name` varchar(200) NOT NULL COMMENT '客户名称快照',
                                       `salesman_id` varchar(32) NOT NULL COMMENT '业务员用户ID，逻辑关联quote_auth.sys_user',
                                       `salesman_name` varchar(100) NOT NULL COMMENT '业务员姓名快照',
                                       `create_date` date NOT NULL COMMENT '订单创建日期/统计日期',
                                       `currency` varchar(3) NOT NULL COMMENT '币种：CNY或USD',
                                       `total_pcs` bigint NOT NULL DEFAULT '0' COMMENT '订单实物总件数',
                                       `total_sets` bigint NOT NULL DEFAULT '0' COMMENT '订单套装总套数',
                                       `total_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT '订单总金额',
                                       `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE有效/CANCELLED已取消',
                                       `cancel_time` datetime DEFAULT NULL COMMENT '取消时间',
                                       `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
                                       `remark` varchar(500) DEFAULT NULL COMMENT '订单备注',
                                       `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                       `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
                                       `creator` varchar(64) NOT NULL COMMENT '创建人账号',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_signed_order_main_order_no` (`order_no`),
                                       KEY `idx_signed_order_main_customer_name` (`customer_name`),
                                       KEY `idx_signed_order_main_salesman` (`salesman_id`),
                                       KEY `idx_signed_order_main_create_date` (`create_date`),
                                       KEY `idx_signed_order_main_total_amount` (`total_amount`),
                                       KEY `idx_signed_order_main_stat` (`status`,`currency`,`create_date`),
                                       CONSTRAINT `fk_signed_order_main_customer`
                                           FOREIGN KEY (`customer_id`) REFERENCES `t_customer` (`id`),
                                       CONSTRAINT `chk_signed_order_main_currency`
                                           CHECK (`currency` IN ('CNY','USD')),
                                       CONSTRAINT `chk_signed_order_main_status`
                                           CHECK (`status` IN ('ACTIVE','CANCELLED')),
                                       CONSTRAINT `chk_signed_order_main_totals`
                                           CHECK (`total_pcs` >= 0 AND `total_sets` >= 0 AND `total_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已签订单主表';


-- quote.t_signed_order_detail 定义

CREATE TABLE `t_signed_order_detail` (
                                         `id` varchar(32) NOT NULL COMMENT '签单商品明细ID，32位UUID',
                                         `order_id` varchar(32) NOT NULL COMMENT '签单主表ID',
                                         `sort_no` int NOT NULL COMMENT '商品显示顺序',
                                         `line_type` varchar(20) NOT NULL COMMENT '类型：SINGLE单品/SET套装',
                                         `product_code` varchar(100) DEFAULT NULL COMMENT '单品代号；SET父行为空',
                                         `weight` decimal(18,3) NOT NULL COMMENT 'SINGLE为单件重量，SET为每套总重量',
                                         `total_pcs` bigint NOT NULL COMMENT '本行实物总件数',
                                         `total_sets` bigint NOT NULL DEFAULT '0' COMMENT 'SET订购套数；SINGLE为0',
                                         `unit_price` decimal(18,4) NOT NULL COMMENT 'SINGLE为单件价格，SET为整套价格',
                                         `amount` decimal(18,2) NOT NULL COMMENT '本行金额',
                                         `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         PRIMARY KEY (`id`),
                                         UNIQUE KEY `uk_signed_order_detail_sort` (`order_id`,`sort_no`),
                                         KEY `idx_signed_order_detail_type` (`line_type`),
                                         KEY `idx_signed_order_detail_product` (`product_code`),
                                         CONSTRAINT `fk_signed_order_detail_main`
                                             FOREIGN KEY (`order_id`) REFERENCES `t_signed_order_main` (`id`) ON DELETE CASCADE,
                                         CONSTRAINT `chk_signed_order_detail_type`
                                             CHECK (`line_type` IN ('SINGLE','SET')),
                                         CONSTRAINT `chk_signed_order_detail_values`
                                             CHECK (`weight` > 0 AND `total_pcs` > 0 AND `total_sets` >= 0
                                                 AND `unit_price` >= 0 AND `amount` >= 0),
                                         CONSTRAINT `chk_signed_order_detail_semantics`
                                             CHECK (
                                                 (`line_type` = 'SINGLE' AND `product_code` IS NOT NULL AND `total_sets` = 0)
                                                 OR
                                                 (`line_type` = 'SET' AND `product_code` IS NULL AND `total_sets` > 0)
                                             )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已签订单计价明细表';


-- quote.t_signed_order_set_item 定义

CREATE TABLE `t_signed_order_set_item` (
                                           `id` varchar(32) NOT NULL COMMENT '套装组成ID，32位UUID',
                                           `order_detail_id` varchar(32) NOT NULL COMMENT '所属SET签单明细ID',
                                           `sort_no` int NOT NULL COMMENT '组成商品显示顺序',
                                           `product_code` varchar(100) NOT NULL COMMENT '组成商品代号',
                                           `weight` decimal(18,3) NOT NULL COMMENT '组成商品单件重量',
                                           `qty_per_set` int NOT NULL COMMENT '每套包含该商品的件数',
                                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (`id`),
                                           UNIQUE KEY `uk_signed_order_set_item_sort` (`order_detail_id`,`sort_no`),
                                           UNIQUE KEY `uk_signed_order_set_item_product` (`order_detail_id`,`product_code`),
                                           KEY `idx_signed_order_set_item_product_code` (`product_code`),
                                           CONSTRAINT `fk_signed_order_set_item_detail`
                                               FOREIGN KEY (`order_detail_id`) REFERENCES `t_signed_order_detail` (`id`) ON DELETE CASCADE,
                                           CONSTRAINT `chk_signed_order_set_item_values`
                                               CHECK (`weight` > 0 AND `qty_per_set` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='已签订单套装组成表';
