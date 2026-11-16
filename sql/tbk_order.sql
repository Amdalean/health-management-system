-- ----------------------------
-- 联盟订单表 + PC 收益看板菜单 + 同步定时任务
-- add by CYQ 2026-11-09 联盟收益 PC 看板
--
-- 说明：
--   1. 淘宝客订单查询接口是邀请制，未开通时同步任务只记录提示，不影响拼多多；
--   2. 定时任务默认“暂停”(status='1')，确认权限/数据正常后在「系统监控-定时任务」里启动；
--   3. 菜单挂在顶级目录「联盟收益」下，角色 100（开发者）/103（生活管家）默认授权。
-- ----------------------------

CREATE TABLE IF NOT EXISTS `tbk_order` (
  `id`                   bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `platform`             varchar(16)   NOT NULL COMMENT '平台：taobao/pdd',
  `order_id`             varchar(64)   NOT NULL COMMENT '平台订单号',
  `item_id`              varchar(64)   DEFAULT NULL COMMENT '商品ID',
  `item_title`           varchar(500)  DEFAULT NULL COMMENT '商品标题',
  `item_img`             varchar(500)  DEFAULT NULL COMMENT '商品主图',
  `shop_name`            varchar(200)  DEFAULT NULL COMMENT '店铺名称',
  `pid`                  varchar(64)   DEFAULT NULL COMMENT '推广位',
  `status`               varchar(16)   DEFAULT NULL COMMENT '平台订单状态码',
  `status_desc`          varchar(64)   DEFAULT NULL COMMENT '状态说明',
  `total_amount`         decimal(12,2) DEFAULT NULL COMMENT '订单总金额',
  `pay_amount`           decimal(12,2) DEFAULT NULL COMMENT '实付金额',
  `estimated_commission` decimal(12,2) DEFAULT NULL COMMENT '预估佣金',
  `settled_commission`   decimal(12,2) DEFAULT NULL COMMENT '结算佣金',
  `commission_rate`      decimal(8,2)  DEFAULT NULL COMMENT '佣金比例（%）',
  `pay_time`             datetime      DEFAULT NULL COMMENT '付款时间',
  `settle_time`          datetime      DEFAULT NULL COMMENT '结算时间',
  `refund_tag`           varchar(32)   DEFAULT NULL COMMENT '退款/风控标记',
  `raw_data`             mediumtext    COMMENT '平台原始报文',
  `create_time`          datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`          datetime      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tbk_order_platform_order` (`platform`, `order_id`),
  KEY `idx_tbk_order_pay_time` (`pay_time`),
  KEY `idx_tbk_order_platform` (`platform`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='联盟订单';

-- ----------------------------
-- PC 菜单：联盟收益 / 订单明细
-- ----------------------------
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '联盟收益', 0, 6, 'tbk', NULL, '', '',
       1, 0, 'M', '0', '0', NULL, 'money',
       'admin', sysdate(), '', NULL, '联盟推广收益目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_name = '联盟收益' AND parent_id = 0);

SET @tbkParentId = (SELECT menu_id FROM sys_menu WHERE menu_name = '联盟收益' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '订单明细', @tbkParentId, 1, 'order', 'tbk/order/index', '', '',
       1, 0, 'C', '0', '0', 'main:tbk:order:list', 'list',
       'admin', sysdate(), '', NULL, '联盟订单与收益明细'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:tbk:order:list');

SET @tbkOrderMenuId = (SELECT menu_id FROM sys_menu WHERE perms = 'main:tbk:order:list' LIMIT 1);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, update_by, update_time, remark)
SELECT '订单同步', @tbkOrderMenuId, 1, '#', '', '', '',
       1, 0, 'F', '0', '0', 'main:tbk:order:sync', 'refresh',
       'admin', sysdate(), '', NULL, '手动同步联盟订单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:tbk:order:sync');

-- 授权给 开发者(100)、生活管家全权(103)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id = @tbkParentId
               OR m.parent_id = @tbkOrderMenuId
               OR m.menu_id = @tbkOrderMenuId
WHERE r.role_id IN (100, 103)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

-- ----------------------------
-- 定时任务：每 30 分钟同步一次（默认暂停）
-- ----------------------------
INSERT INTO sys_job
  (job_name, job_group, invoke_target, cron_expression, misfire_policy, concurrent, status,
   create_by, create_time, update_by, update_time, remark)
SELECT '联盟订单同步', 'SYSTEM', 'tbkOrderSyncTask.excute()', '0 0/30 * * * ?', '3', '1', '1',
       'admin', sysdate(), '', NULL, '每30分钟同步淘宝客/拼多多联盟订单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_job WHERE invoke_target = 'tbkOrderSyncTask.excute()');

-- 执行后检查
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--  WHERE menu_name IN ('联盟收益','订单明细','订单同步');
-- SELECT job_id, job_name, invoke_target, cron_expression, status FROM sys_job
--  WHERE invoke_target = 'tbkOrderSyncTask.excute()';
