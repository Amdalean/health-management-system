-- ----------------------------
-- 随手记迁移到财务中心
-- add by CYQ 2026-09-02 说明: 将随手记(2026)从"资料库"目录(2025)迁移到"财务中心"顶级目录(20817)
-- 幂等: 重复执行不会重复迁移
-- ----------------------------

SET @financeId = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' LIMIT 1);
SET @jotdownId = 2026;

-- 1. 迁移随手记菜单到财务中心（order 3，排在财务汇总1、量化交易2之后）
UPDATE sys_menu SET parent_id = @financeId, order_num = 3
WHERE menu_id = @jotdownId AND parent_id <> @financeId;

-- 2. 将财务中心目录分配给所有拥有随手记的角色（父目录必须分配，否则菜单树不显示）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @financeId
FROM sys_role r
WHERE r.role_id IN (SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id = @jotdownId)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = r.role_id AND rm2.menu_id = @financeId);

-- 验证
SELECT menu_id, menu_name, parent_id, order_num, path, component, menu_type, icon
FROM sys_menu
WHERE menu_id IN (@financeId, @jotdownId, 2032, 20788)
   OR parent_id = @financeId
ORDER BY parent_id, order_num, menu_id;
