-- ----------------------------
-- 生活管家重构（原我的管家/财务中心）
-- add by CYQ 2026-09-02 说明: 删除学生信息节点，迁移提醒管理与时间管理局到生活管家(原财务中心/我的管家)，并将财务中心更名为"生活管家"
-- 幂等: 重复执行安全
-- ----------------------------

SET @myButlerId = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'finance' LIMIT 1);

-- 1. 更名财务中心 -> 生活管家（服务个人，非企业；2026-09-02 从我的管家调整为生活管家）
-- icon 需为 src/assets/icons/svg 下存在的 svg（house 不存在，money/user/peoples 等才有效）
UPDATE sys_menu SET menu_name = '生活管家', icon = 'peoples'
WHERE menu_id = @myButlerId AND menu_name <> '生活管家';

-- 2. 删除学生信息节点及其按钮（2019 + 2020-2024）
DELETE FROM sys_role_menu WHERE menu_id IN (2019,2020,2021,2022,2023,2024);
DELETE FROM sys_menu WHERE menu_id IN (2019,2020,2021,2022,2023,2024);

-- 3. 迁移提醒管理(2057)与时间管理局(20811)到我的管家
UPDATE sys_menu SET parent_id = @myButlerId, order_num = 4
WHERE menu_id = 2057 AND parent_id <> @myButlerId;

UPDATE sys_menu SET parent_id = @myButlerId, order_num = 5
WHERE menu_id = 20811 AND parent_id <> @myButlerId;

-- 4. 将生活管家目录分配给所有拥有提醒/时间管理局的角色（父目录必须分配）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @myButlerId
FROM sys_role r
WHERE r.role_id IN (SELECT DISTINCT role_id FROM sys_role_menu WHERE menu_id IN (2057, 20811))
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm2 WHERE rm2.role_id = r.role_id AND rm2.menu_id = @myButlerId);

-- 验证
SELECT menu_id, menu_name, parent_id, order_num, path, icon FROM sys_menu WHERE menu_id = @myButlerId
UNION ALL
SELECT menu_id, menu_name, parent_id, order_num, path, icon FROM sys_menu WHERE parent_id = @myButlerId ORDER BY order_num;
