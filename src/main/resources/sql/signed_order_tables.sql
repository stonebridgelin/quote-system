-- 签单管理模块建表脚本
-- 适用于已有 quote 数据库：只新建签单模块的四张表，不修改现有报价表。

CREATE TABLE `t_customer` (
                              `id` varchar(32) NOT NULL COMMENT '客户ID，32位UUID',
                              `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
                              `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_customer_name` (`customer_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户基础表';

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
