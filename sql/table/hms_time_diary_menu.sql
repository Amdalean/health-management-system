-- ----------------------------
-- 时间管理局菜单注册
-- add by CYQ 2026年8月4日
-- 使用说明:
--   1. 请先查询父菜单ID: SELECT menu_id FROM sys_menu WHERE menu_name = '业务管理';
--      如果没有"业务管理"目录，可挂在其他合适的一级/二级目录下
--   2. 将脚本中的 @parentId 替换为实际的父菜单ID
--   3. 执行后，可在PC端"角色管理"中为角色分配这些菜单权限
-- ----------------------------

-- 插入页面菜单（若已存在则跳过）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局', @parentId, 10, 'timeDiary', 'main/timeDiary/index', NULL, 1, 0, 'C', '0', '0', 'main:timeDiary:list', 'date', 'admin', sysdate(), '', NULL, ''
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:timeDiary:list' AND menu_type = 'C');

-- 插入按钮权限
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局查询', menu_id, 1, '', '', NULL, 1, 0, 'F', '0', '0', 'main:timeDiary:query', '', 'admin', sysdate(), '', NULL, ''
FROM sys_menu WHERE perms = 'main:timeDiary:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局新增', menu_id, 2, '', '', NULL, 1, 0, 'F', '0', '0', 'main:timeDiary:add', '', 'admin', sysdate(), '', NULL, ''
FROM sys_menu WHERE perms = 'main:timeDiary:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局修改', menu_id, 3, '', '', NULL, 1, 0, 'F', '0', '0', 'main:timeDiary:edit', '', 'admin', sysdate(), '', NULL, ''
FROM sys_menu WHERE perms = 'main:timeDiary:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局删除', menu_id, 4, '', '', NULL, 1, 0, 'F', '0', '0', 'main:timeDiary:remove', '', 'admin', sysdate(), '', NULL, ''
FROM sys_menu WHERE perms = 'main:timeDiary:list';

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT '时间管理局导出', menu_id, 5, '', '', NULL, 1, 0, 'F', '0', '0', 'main:timeDiary:export', '', 'admin', sysdate(), '', NULL, ''
FROM sys_menu WHERE perms = 'main:timeDiary:list';
