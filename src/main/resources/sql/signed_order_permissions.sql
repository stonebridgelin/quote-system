-- 签单管理权限初始化脚本（一次性执行）
-- 一级权限代表业务模块，所有Controller方法权限均作为其二级权限。

SET @signed_order_page_id = REPLACE(UUID(), '-', '');
SET @signed_order_list_id = REPLACE(UUID(), '-', '');
SET @signed_order_create_id = REPLACE(UUID(), '-', '');
SET @signed_order_detail_id = REPLACE(UUID(), '-', '');
SET @signed_order_cancel_id = REPLACE(UUID(), '-', '');
SET @signed_order_customer_options_id = REPLACE(UUID(), '-', '');
SET @signed_order_salesman_options_id = REPLACE(UUID(), '-', '');
SET @admin_role_id = '5510bc7c811c11f196fa005056c00001';

INSERT INTO quote_auth.sys_permissions
(id, parent_id, permission_name, permission_code, permission_type, module_code,
 action_code, route_path, route_name, component_path, redirect_path, icon,
 visible, keep_alive, show_in_home, home_title, home_description, button_key,
 api_method, api_path, sort_value, status, remark, meta_json, is_deleted,
 create_time, create_by, update_time, update_by)
VALUES
(@signed_order_page_id, '0', '签单管理', 'signed-order:page', 1, 'signed-order',
 'page', '/signed-order', 'SignedOrder', 'signedOrder/SignedOrderList', NULL, NULL,
 1, 0, 1, '签单管理', '管理和统计已经签下的订单', NULL,
 NULL, NULL, 60, 1, '签单管理一级页面权限', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_list_id, @signed_order_page_id, '查询签单列表', 'signed-order:list', 2, 'signed-order',
 'list', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:list',
 'POST', '/api/signed-order/page', 61, 1, '分页筛选签单并统计人民币、美元总额', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_create_id, @signed_order_page_id, '新增签单', 'signed-order:create', 2, 'signed-order',
 'create', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:create',
 'POST', '/api/signed-order/create', 62, 1, '新增签单、商品明细和套装组成', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_detail_id, @signed_order_page_id, '查看签单详情', 'signed-order:detail', 2, 'signed-order',
 'detail', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:detail',
 'GET', '/api/signed-order/detail/{id}', 63, 1, '查看签单商品及套装组成', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_cancel_id, @signed_order_page_id, '取消签单', 'signed-order:cancel', 2, 'signed-order',
 'cancel', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:cancel',
 'POST', '/api/signed-order/cancel/{id}', 64, 1, '取消订单并排除有效金额统计', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_customer_options_id, @signed_order_page_id, '查询客户选项', 'signed-order:customer-options', 2, 'signed-order',
 'customer-options', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:customer-options',
 'GET', '/api/signed-order/customer/options', 65, 1, '模糊查询客户下拉选项', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL),

(@signed_order_salesman_options_id, @signed_order_page_id, '查询业务员选项', 'signed-order:salesman-options', 2, 'signed-order',
 'salesman-options', NULL, NULL, NULL, NULL, NULL,
 1, 0, 0, NULL, NULL, 'signed-order:salesman-options',
 'GET', '/api/signed-order/salesman/options', 66, 1, '查询启用状态的业务员下拉选项', NULL, 0,
 CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL);

-- 为admin角色授予签单管理模块的全部权限。
INSERT INTO quote_auth.sys_role_permission
(id, role_id, permission_id, is_deleted, create_time, create_by, update_time, update_by)
SELECT REPLACE(UUID(), '-', ''), @admin_role_id, p.id, 0,
       CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP, NULL
FROM quote_auth.sys_permissions AS p
WHERE p.module_code = 'signed-order'
  AND p.is_deleted = 0;
