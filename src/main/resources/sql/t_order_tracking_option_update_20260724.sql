-- 订单重点追踪事项步骤调整
-- 说明：
-- 1. 保留所有既有 option_value，避免已保存订单和历史日志的状态语义改变。
-- 2. 新增的“设计初步完成，已发客户待确认”使用中间状态码。
-- 3. 原“检查无误”仅停用，不物理删除，供历史数据翻译使用。
-- 4. 本脚本按业务键判断新增，可重复执行。
-- 5. 必须先迁移主表当前状态，再停用旧选项；历史日志不迁移。

START TRANSACTION;

-- 修复旧脚本已执行或旧订单仍使用“检查无误”的情况。
-- 这些 UPDATE 放在停用配置之前；重复执行不会产生额外影响。
UPDATE quote.t_order_tracking
SET sticker_status = 3400
WHERE sticker_status = 3500;

UPDATE quote.t_order_tracking
SET inner_box_status = 5400
WHERE inner_box_status = 5500;

UPDATE quote.t_order_tracking
SET color_box_status = 7400
WHERE color_box_status = 7500;

UPDATE quote.t_order_tracking
SET carton_status = 1400
WHERE carton_status = 1500;

-- 底标：有底标（parent_value = 20）
UPDATE quote.t_order_tracking_option
SET option_label = '客户已提供设计资料',
    sort_no = 20,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'BOTTOM_LABEL'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 8200;

INSERT INTO quote.t_order_tracking_option
    (id, module_type, option_type, parent_value, option_value, option_label,
     sort_no, tag_type, remark, is_enabled, is_deleted, create_time, create_by)
SELECT
    '6123636d60c74136b73973328b64bfec', 'BOTTOM_LABEL', 'STATUS', 20, 8250,
    '设计初步完成，已发客户待确认', 30, 'warning',
    '底标设计初稿已发送客户，等待确认', 1, 0, NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM quote.t_order_tracking_option
    WHERE module_type = 'BOTTOM_LABEL'
      AND option_type = 'STATUS'
      AND parent_value = 20
      AND option_value = 8250
);

UPDATE quote.t_order_tracking_option
SET option_label = '客户已确认',
    sort_no = 40,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'BOTTOM_LABEL'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 8300;

UPDATE quote.t_order_tracking_option
SET sort_no = 50,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'BOTTOM_LABEL'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 8400;

-- 不干胶：工厂生产（parent_value = 30）
UPDATE quote.t_order_tracking_option
SET option_label = '客户已提供设计资料',
    sort_no = 20,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'STICKER'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 3200;

INSERT INTO quote.t_order_tracking_option
    (id, module_type, option_type, parent_value, option_value, option_label,
     sort_no, tag_type, remark, is_enabled, is_deleted, create_time, create_by)
SELECT
    '375480b967e54dc28bed429e3636e692', 'STICKER', 'STATUS', 30, 3250,
    '设计初步完成，已发客户待确认', 30, 'warning',
    '不干胶设计初稿已发送客户，等待确认', 1, 0, NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM quote.t_order_tracking_option
    WHERE module_type = 'STICKER'
      AND option_type = 'STATUS'
      AND parent_value = 30
      AND option_value = 3250
);

UPDATE quote.t_order_tracking_option
SET sort_no = 40,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'STICKER'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 3300;

UPDATE quote.t_order_tracking_option
SET sort_no = 50,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'STICKER'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 3400;

UPDATE quote.t_order_tracking_option
SET sort_no = 60,
    tag_type = 'info',
    remark = '历史状态，已停用，不再出现在新流程中',
    is_enabled = 0,
    is_deleted = 0,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'STICKER'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 3500;

-- 黄盒：印刷（parent_value = 20）
UPDATE quote.t_order_tracking_option
SET option_label = '客户已提供设计资料',
    sort_no = 20,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'INNER_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 5200;

INSERT INTO quote.t_order_tracking_option
    (id, module_type, option_type, parent_value, option_value, option_label,
     sort_no, tag_type, remark, is_enabled, is_deleted, create_time, create_by)
SELECT
    'ab4b4b0c354b4be0b23141bdd3305d5e', 'INNER_BOX', 'STATUS', 20, 5250,
    '设计初步完成，已发客户待确认', 30, 'warning',
    '黄盒设计初稿已发送客户，等待确认', 1, 0, NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM quote.t_order_tracking_option
    WHERE module_type = 'INNER_BOX'
      AND option_type = 'STATUS'
      AND parent_value = 20
      AND option_value = 5250
);

UPDATE quote.t_order_tracking_option
SET sort_no = 40,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'INNER_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 5300;

UPDATE quote.t_order_tracking_option
SET sort_no = 50,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'INNER_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 5400;

UPDATE quote.t_order_tracking_option
SET sort_no = 60,
    tag_type = 'info',
    remark = '历史状态，已停用，不再出现在新流程中',
    is_enabled = 0,
    is_deleted = 0,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'INNER_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 20
  AND option_value = 5500;

-- 彩盒：工厂生产（parent_value = 30）
UPDATE quote.t_order_tracking_option
SET option_label = '客户已提供设计资料',
    sort_no = 20,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'COLOR_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 7200;

INSERT INTO quote.t_order_tracking_option
    (id, module_type, option_type, parent_value, option_value, option_label,
     sort_no, tag_type, remark, is_enabled, is_deleted, create_time, create_by)
SELECT
    'd7dd014d356d478e9683c6b1bac09351', 'COLOR_BOX', 'STATUS', 30, 7250,
    '设计初步完成，已发客户待确认', 30, 'warning',
    '彩盒设计初稿已发送客户，等待确认', 1, 0, NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM quote.t_order_tracking_option
    WHERE module_type = 'COLOR_BOX'
      AND option_type = 'STATUS'
      AND parent_value = 30
      AND option_value = 7250
);

UPDATE quote.t_order_tracking_option
SET sort_no = 40,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'COLOR_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 7300;

UPDATE quote.t_order_tracking_option
SET sort_no = 50,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'COLOR_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 7400;

UPDATE quote.t_order_tracking_option
SET sort_no = 60,
    tag_type = 'info',
    remark = '历史状态，已停用，不再出现在新流程中',
    is_enabled = 0,
    is_deleted = 0,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'COLOR_BOX'
  AND option_type = 'STATUS'
  AND parent_value = 30
  AND option_value = 7500;

-- 外箱：无父级类型
UPDATE quote.t_order_tracking_option
SET option_label = '客户已提供设计资料',
    sort_no = 20,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'CARTON'
  AND option_type = 'STATUS'
  AND parent_value IS NULL
  AND option_value = 1200;

INSERT INTO quote.t_order_tracking_option
    (id, module_type, option_type, parent_value, option_value, option_label,
     sort_no, tag_type, remark, is_enabled, is_deleted, create_time, create_by)
SELECT
    '0ca78cf0f4434c0593db0a97a16065dc', 'CARTON', 'STATUS', NULL, 1250,
    '设计初步完成，已发客户待确认', 30, 'warning',
    '外箱设计初稿已发送客户，等待确认', 1, 0, NOW(), 'system'
WHERE NOT EXISTS (
    SELECT 1
    FROM quote.t_order_tracking_option
    WHERE module_type = 'CARTON'
      AND option_type = 'STATUS'
      AND parent_value IS NULL
      AND option_value = 1250
);

UPDATE quote.t_order_tracking_option
SET sort_no = 40,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'CARTON'
  AND option_type = 'STATUS'
  AND parent_value IS NULL
  AND option_value = 1300;

UPDATE quote.t_order_tracking_option
SET sort_no = 50,
    tag_type = 'success',
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'CARTON'
  AND option_type = 'STATUS'
  AND parent_value IS NULL
  AND option_value = 1400;

UPDATE quote.t_order_tracking_option
SET sort_no = 60,
    tag_type = 'info',
    remark = '历史状态，已停用，不再出现在新流程中',
    is_enabled = 0,
    is_deleted = 0,
    update_time = NOW(),
    update_by = 'system'
WHERE module_type = 'CARTON'
  AND option_type = 'STATUS'
  AND parent_value IS NULL
  AND option_value = 1500;

COMMIT;

-- 验证：以下四个 remaining_count 均应为 0。
SELECT 'sticker_status_3500' AS check_item, COUNT(*) AS remaining_count
FROM quote.t_order_tracking
WHERE sticker_status = 3500
UNION ALL
SELECT 'inner_box_status_5500', COUNT(*)
FROM quote.t_order_tracking
WHERE inner_box_status = 5500
UNION ALL
SELECT 'color_box_status_7500', COUNT(*)
FROM quote.t_order_tracking
WHERE color_box_status = 7500
UNION ALL
SELECT 'carton_status_1500', COUNT(*)
FROM quote.t_order_tracking
WHERE carton_status = 1500;

-- 验证：五个受影响流程应各返回 5 个启用状态。
SELECT module_type, parent_value, COUNT(*) AS enabled_status_count
FROM quote.t_order_tracking_option
WHERE is_deleted = 0
  AND is_enabled = 1
  AND option_type = 'STATUS'
  AND (
      (module_type = 'BOTTOM_LABEL' AND parent_value = 20)
      OR (module_type = 'STICKER' AND parent_value = 30)
      OR (module_type = 'INNER_BOX' AND parent_value = 20)
      OR (module_type = 'COLOR_BOX' AND parent_value = 30)
      OR (module_type = 'CARTON' AND parent_value IS NULL)
  )
GROUP BY module_type, parent_value
ORDER BY module_type, parent_value;

-- 执行完成后，请重启后端，或调用：
-- POST /order-tracking/translate/cache/refresh
