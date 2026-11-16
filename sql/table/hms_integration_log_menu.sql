-- ----------------------------
-- 系统集成日志菜单（系统管理 -> 日志管理 -> 集成日志）
-- add by CYQ 2026-09-14 说明: 系统集成日志菜单注册
-- ----------------------------
-- 父菜单「日志管理」的 menu_id 一般为 108，此处按 menu_name 动态查询，避免环境差异
SET @parentId = (SELECT menu_id FROM sys_menu WHERE menu_name = '日志管理' AND menu_type = 'M' LIMIT 1);

-- 插入菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
('集成日志', @parentId, 3, 'integrationlog', 'monitor/integrationlog/index', NULL, 1, 0, 'C', '0', '0', 'monitor:integrationlog:list', 'form', 'admin', sysdate(), '', NULL, '系统集成日志菜单');

-- 查询按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '集成日志查询', menu_id, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:integrationlog:query', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'monitor:integrationlog:list';

-- 删除按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '集成日志删除', menu_id, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:integrationlog:remove', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'monitor:integrationlog:list';

-- 导出按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '集成日志导出', menu_id, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'monitor:integrationlog:export', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'monitor:integrationlog:list';
