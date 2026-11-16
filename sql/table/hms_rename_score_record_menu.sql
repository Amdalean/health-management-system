-- ----------------------------
-- 菜单显示名调整：成绩分析记录 → 成绩智能分析
-- add by CYQ 2026-10-18 说明:
--   仅调整 sys_menu 的显示名称（菜单名 + 下级按钮名），
--   表 hms_score_record、实体 HmsScoreRecord、权限标识 main:scoreRecord:* 保持不变。
--   幂等：可重复执行。
-- ----------------------------

SET @scoreMenu = (SELECT `menu_id` FROM `sys_menu` WHERE `perms` = 'main:scoreRecord:list' LIMIT 1);

-- 1. 主菜单名
UPDATE `sys_menu` SET `menu_name` = '成绩智能分析'
WHERE `menu_id` = @scoreMenu;

-- 2. 下级按钮名：成绩分析记录查询 → 成绩智能分析查询 …（'提交' 不含旧名，保持原样）
UPDATE `sys_menu` SET `menu_name` = REPLACE(`menu_name`, '成绩分析记录', '成绩智能分析')
WHERE `parent_id` = @scoreMenu;

-- 3. 校验
SELECT `menu_id`, `menu_name`, `parent_id`, `perms`
FROM `sys_menu`
WHERE `menu_id` = @scoreMenu OR `parent_id` = @scoreMenu
ORDER BY `parent_id`, `order_num`;
