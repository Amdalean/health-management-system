-- ----------------------------
-- 重命名财务菜单：财务信息录入 -> 财务汇总
-- add by CYQ 2026-08-31 说明: 统一命名为资源型，与 Entity/Controller(hsm_summary/财务汇总主)一致
-- 幂等: 重复执行不会产生副作用
-- ----------------------------
UPDATE sys_menu SET menu_name = '财务汇总' WHERE menu_id = 2032 AND menu_name <> '财务汇总';

-- 验证
SELECT menu_id, menu_name, parent_id, path, component, icon FROM sys_menu WHERE menu_id = 2032;
