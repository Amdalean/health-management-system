-- ----------------------------
-- 知识地图菜单注册
-- add by CYQ 2026-08-13 说明: 在"资料库"(2025)目录下新增"知识地图"菜单，
--   前端组件 main/knowledgemap/index，数据来源为资料库(hms_document)层级数据
-- add by CYQ 2026-10-18 说明: "资料库"目录已合并进"教学管理"，
--   父级改为按 path 动态解析（优先 teaching，回退旧的 main），避免硬编码 2025 失效。
-- 幂等: 重复执行不会重复创建菜单、不会重复分配角色
-- ----------------------------

SET @kmParent = IFNULL(
    (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'teaching' LIMIT 1),
    (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'main' LIMIT 1)
);

-- 1. 创建"知识地图"菜单（若不存在）
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `remark`)
SELECT '知识地图', @kmParent, 6, 'knowledgemap', 'main/knowledgemap/index', '', '', 1, 0, 'C', '0', '0',
       'main:knowledgemap:list', 'tree', 'admin', NOW(), '知识地图菜单'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE `path` = 'knowledgemap');

SET @kmId = (SELECT menu_id FROM sys_menu WHERE `path` = 'knowledgemap' LIMIT 1);

-- 2. 将"知识地图"分配给所有拥有"资料库管理"(2044)菜单的角色（数据同源，权限跟随）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @kmId
FROM sys_role r
WHERE r.role_id IN (SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id = 2044)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = r.role_id AND rm2.menu_id = @kmId);

-- 验证
SELECT m.menu_id, m.menu_name, m.parent_id, m.order_num, m.path, m.component, m.perms, m.icon
FROM sys_menu m
WHERE m.menu_id = @kmId;

SELECT rm.role_id, r.role_name
FROM sys_role_menu rm JOIN sys_role r ON r.role_id = rm.role_id
WHERE rm.menu_id = @kmId;
