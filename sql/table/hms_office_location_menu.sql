-- ----------------------------
-- 办公地点管理菜单
-- add by CYQ 2026-06-06 说明: 办公地点管理菜单注册
-- ----------------------------
-- 请先查询父菜单ID:
-- SELECT menu_id FROM sys_menu WHERE menu_name = '移动端管理';

-- 插入菜单（请将 @parentId 替换为实际的移动端管理菜单ID）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
('办公地点', @parentId, 4, 'officeLocation', 'mobile/officeLocation/index', NULL, 1, 0, 'C', '0', '0', 'mobile:officeLocation:list', 'location', 'admin', sysdate(), '', NULL, '');

-- 插入按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '办公地点查询', menu_id, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:officeLocation:query', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:officeLocation:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '办公地点新增', menu_id, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:officeLocation:add', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:officeLocation:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '办公地点修改', menu_id, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:officeLocation:edit', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:officeLocation:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '办公地点删除', menu_id, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:officeLocation:remove', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:officeLocation:list';

-- 打卡记录菜单追加导出按钮权限 (如果尚未注册)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '打卡记录导出', menu_id, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:clockin:export', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:clockin:list';
