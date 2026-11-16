-- ----------------------------
-- 打卡记录管理菜单
-- ----------------------------
-- 父菜单：移动端管理 (假设 parent_id 为已有移动端管理菜单ID，需根据实际情况调整)
-- 请先查询父菜单ID:
-- SELECT menu_id FROM sys_menu WHERE menu_name = '移动端管理';

-- 插入菜单（请将 @parentId 替换为实际的移动端管理菜单ID）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES
('打卡记录', @parentId, 3, 'clockin', 'mobile/clockin/index', NULL, 1, 0, 'C', '0', '0', 'mobile:clockin:list', 'checkin', 'admin', sysdate(), '', NULL, '');

-- 插入按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '打卡记录查询', menu_id, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:clockin:query', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:clockin:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '打卡记录删除', menu_id, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'mobile:clockin:remove', '', 'admin', sysdate(), '', NULL, '' FROM sys_menu WHERE perms = 'mobile:clockin:list';
