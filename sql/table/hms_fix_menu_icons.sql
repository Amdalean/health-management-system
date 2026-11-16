-- ----------------------------
-- 修复移动端管理 & 资料库 菜单图标
-- add by CYQ 2026-06-09 说明: 修正不存在的SVG图标引用
-- ----------------------------

-- 移动端管理（父菜单）：mobilephone 不存在 → 改用 phone
UPDATE sys_menu SET icon = 'phone' WHERE menu_name = '移动端管理' AND icon = 'mobilephone';

-- 推送设备管理：# 无图标 → 改用 server
UPDATE sys_menu SET icon = 'server' WHERE menu_name = '推送设备管理' AND (icon = '#' OR icon IS NULL OR icon = '');

-- APP版本管理：# 无图标 → 改用 build
UPDATE sys_menu SET icon = 'build' WHERE menu_name = 'APP版本管理' AND (icon = '#' OR icon IS NULL OR icon = '');

-- 打卡记录：checkin 已创建对应SVG（无需改动，仅确保SVG文件存在）
-- 办公地点：location 已创建对应SVG（无需改动，仅确保SVG文件存在）

-- 资料库父菜单：如果也无图标，补上 documentation
UPDATE sys_menu SET icon = 'documentation' WHERE menu_name = '资料库' AND (icon = '#' OR icon IS NULL OR icon = '');

-- 查看修复结果
SELECT menu_id, menu_name, icon, visible
FROM sys_menu
WHERE menu_name IN ('移动端管理', '推送设备管理', 'APP版本管理', '打卡记录', '办公地点', '资料库')
ORDER BY menu_id;
