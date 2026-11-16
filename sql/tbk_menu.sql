-- ----------------------------
-- 移动端工作台「优惠查询」菜单节点
-- add by CYQ 2026-11-03 淘宝客转链模块
--
-- 说明：
--   1. 挂在「生活管家」(parent_id=20817) 下，menu_type='F'（按钮），
--      只作为权限标识存在：getRouters 只返回 M/C 类型，因此 PC 端不会生成路由、不显示菜单；
--      但 selectMenuPermsByUserId 不区分类型，权限仍会下发给前端；
--   2. 在「系统管理 - 角色管理 - 菜单权限」里勾选「优惠查询」后，
--      移动端工作台（pages/work/index）才会显示该宫格；
--   3. 权限标识为 main:tbk:list，与 pages/work/index.vue 中 allMenus 配置一致。
--
-- 2026-11-03 已执行于生产库（menu_id=20848），授权角色：100 开发者、103 生活管家全权。
-- ----------------------------
INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, query, route_name,
   is_frame, is_cache, menu_type, visible, status, perms, icon,
   create_by, create_time, remark)
SELECT '优惠查询', 20817, 6, '#', '', '', '',
       1, 0, 'F', '0', '0', 'main:tbk:list', 'shopping',
       'admin', sysdate(), '移动端工作台-淘宝客优惠查询'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'main:tbk:list');

-- 授权给 开发者(100)、生活管家全权(103)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r, sys_menu m
WHERE r.role_id IN (100, 103)
  AND m.perms = 'main:tbk:list'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

-- 执行后查看结果
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms, icon
-- FROM sys_menu WHERE perms = 'main:tbk:list';
-- SELECT r.role_id, r.role_name FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE m.perms = 'main:tbk:list';
