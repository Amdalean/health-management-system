-- ----------------------------
-- 修复资料库子菜单图标
-- add by CYQ 2026-06-09
-- ----------------------------
UPDATE sys_menu SET icon = 'documentation' WHERE menu_id = 2044;  -- 资料库管理
UPDATE sys_menu SET icon = 'message'       WHERE menu_id = 2057;  -- 提醒管理
UPDATE sys_menu SET icon = 'chart'         WHERE menu_id = 2063;  -- 成绩分析记录
UPDATE sys_menu SET icon = 'money'         WHERE menu_id = 2032;  -- 财务汇总
UPDATE sys_menu SET icon = 'edit'          WHERE menu_id = 2026;  -- 随手记
UPDATE sys_menu SET icon = 'people'        WHERE menu_id = 2019;  -- 学生信息

-- 验证
SELECT menu_id, menu_name, icon
FROM sys_menu
WHERE menu_id IN (2044, 2057, 2063, 2032, 2026, 2019, 20788);
