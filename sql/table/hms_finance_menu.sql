-- ----------------------------
-- 财务中心菜单拆分
-- add by CYQ 2026-08-13 说明: 将财务相关菜单从"资料库"目录拆出，挂到新的"财务中心"顶级目录
-- 涉及菜单:
--   财务汇总(2032)  -> 财务中心
--   股票分析(20788)     -> 财务中心
-- 幂等: 重复执行不会重复创建、不会重复迁移
-- ----------------------------

-- 1. 创建"财务中心"顶级目录（若不存在）
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
 `create_by`, `create_time`, `remark`)
SELECT '财务中心', 0, 7, 'finance', NULL, '', '', 1, 0, 'M', '0', '0', '', 'money',
       'admin', NOW(), ''
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE parent_id = 0 AND path = 'finance');

SET @financeId = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' LIMIT 1);

-- 2. 移动财务汇总(2032)到财务中心
UPDATE sys_menu SET parent_id = @financeId, order_num = 1
WHERE menu_id = 2032 AND parent_id <> @financeId;

-- 3. 移动股票分析(20788)到财务中心
UPDATE sys_menu SET parent_id = @financeId, order_num = 2
WHERE menu_id = 20788 AND parent_id <> @financeId;

-- 4. 将"财务中心"目录分配给所有拥有财务子菜单的角色（RuoYi 菜单树按 sys_role_menu 关联查询，父目录必须分配）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @financeId
FROM sys_role r
WHERE r.role_id IN (SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id IN (2032, 20788))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = r.role_id AND rm2.menu_id = @financeId);

-- 验证
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, icon
FROM sys_menu
WHERE menu_id IN (@financeId, 2032, 20788)
   OR parent_id IN (@financeId, 2032, 20788)
ORDER BY parent_id, order_num, menu_id;
