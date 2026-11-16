-- ----------------------------
-- 移动端工作台菜单注册
-- add by CYQ 2026-06-06 说明: 移动端工作台宫格入口菜单，用于角色权限控制
-- 使用说明:
--   1. 请先查询父菜单ID: SELECT menu_id FROM sys_menu WHERE menu_name = '移动端管理';
--   2. 将脚本中的 @parentId 替换为实际的父菜单ID
--   3. 执行后，可在PC端"角色管理"中为角色分配这些菜单权限
-- ----------------------------

-- 资料管理 (若已存在则跳过)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '资料管理', @parentId, 1, 'document', 'mobile/document/index', NULL, 1, 0, 'C', '0', '0', 'main:document:list', 'form', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:document:list' AND menu_type = 'C');

-- 提醒管理 (若已存在则跳过)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '提醒管理', @parentId, 2, 'reminder', 'mobile/reminder/index', NULL, 1, 0, 'C', '0', '0', 'main:reminder:list', 'message', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:reminder:list' AND menu_type = 'C');

-- 成绩分析 (若已存在则跳过)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '成绩分析', @parentId, 3, 'scoreRecord', 'mobile/scoreRecord/index', NULL, 1, 0, 'C', '0', '0', 'main:scoreRecord:list', 'chart', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:scoreRecord:list' AND menu_type = 'C');

-- 随手记 (若已存在则跳过)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '随手记', @parentId, 4, 'jotdown', 'mobile/jotdown/index', NULL, 1, 0, 'C', '0', '0', 'main:jotdown:list', 'edit', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:jotdown:list' AND menu_type = 'C');
