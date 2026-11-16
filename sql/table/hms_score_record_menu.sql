-- ----------------------------
-- 成绩分析记录：学生/教师角色补充「新增」权限
-- add by CYQ 2026-10-18 说明:
--   成绩分析记录已与学生建档、课程建档、学生选课联动：
--   学生需要能自行上传视频（新增记录），教师需要能代录，
--   因此给「学生」「教师」角色补授 main:scoreRecord:add。
--   幂等：可重复执行。
-- ----------------------------

SET @roleStudent = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'student' AND `del_flag` = '0' LIMIT 1);
SET @roleTeacher = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'teacher' AND `del_flag` = '0' LIMIT 1);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleStudent, m.`menu_id` FROM `sys_menu` m WHERE m.`perms` = 'main:scoreRecord:add';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleTeacher, m.`menu_id` FROM `sys_menu` m WHERE m.`perms` = 'main:scoreRecord:add';

-- 校验
SELECT r.`role_key`, m.`menu_name`, m.`perms`
FROM `sys_role` r
JOIN `sys_role_menu` rm ON rm.`role_id` = r.`role_id`
JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE r.`role_key` IN ('student', 'teacher') AND m.`perms` LIKE 'main:scoreRecord%'
ORDER BY r.`role_key`, m.`menu_id`;
