-- 量化交易专用角色：按角色推送消息，不再按持仓判断
-- add by CYQ 2026-08-21

-- 1. 创建角色 quant（幂等：role_key 已存在则不重复插入）
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '量化交易用户', 'quant', 8, '1', 1, 1, '0', '0', 'admin', NOW(), '接收量化交易邮件/持仓分析与全市场扫描的专用角色，按角色而非持仓判断'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key = 'quant');

-- 2. 绑定量化交易菜单（幂等）
-- 财务中心 20817 + 量化交易 20788 及其子菜单 20789,20805,20807,20809,20810
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN (
    SELECT 20817 AS menu_id UNION ALL SELECT 20788 UNION ALL SELECT 20789
    UNION ALL SELECT 20805 UNION ALL SELECT 20807
    UNION ALL SELECT 20809 UNION ALL SELECT 20810
) m
WHERE r.role_key = 'quant';

-- 3. 迁移：现有持仓用户自动授予 quant 角色（保持历史行为，幂等）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT DISTINCT h.user_id, r.role_id
FROM stock_holding h
JOIN sys_role r ON r.role_key = 'quant'
WHERE h.user_id IS NOT NULL;

-- 4. 兜底：若无持仓用户（全新环境），给 101/CYQ 一个示例授权（幂等，不影响已存在）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 101, r.role_id FROM sys_role r WHERE r.role_key = 'quant' AND EXISTS (SELECT 1 FROM sys_user WHERE user_id = 101);
