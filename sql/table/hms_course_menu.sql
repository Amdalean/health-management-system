-- ----------------------------
-- 「课程管理」菜单注册
-- add by CYQ 2026-10-18 说明:
--   在「教学管理」目录（path=teaching）下新增「课程管理」菜单及功能按钮，
--   并为「教师」「学生」角色授予只读权限（学生可查看课程）。
--   幂等：可重复执行。
-- ----------------------------

SET @teachingDir = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'teaching' LIMIT 1);

-- ========== 1. 课程管理菜单 ==========
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '课程管理', @teachingDir, 3, 'course', 'main/course/index', '', '', 1, 0, 'C', '0', '0',
       'main:course:list', 'tree-table', 'admin', NOW(), '课程建档与选课管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'course');

SET @courseMenu = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'course' LIMIT 1);

-- ========== 2. 功能按钮 ==========
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT t.nm, t.pid, t.ord, '', '', '', '', 1, 0, 'F', '0', '0', t.pm, '#', 'admin', NOW(), t.rm
FROM (
  SELECT '课程查询'   AS nm, @courseMenu AS pid, 1 AS ord, 'main:course:query'  AS pm, '课程查询按钮' AS rm
  UNION ALL SELECT '课程新增',   @courseMenu, 2, 'main:course:add',    '课程新增按钮'
  UNION ALL SELECT '课程修改',   @courseMenu, 3, 'main:course:edit',   '课程修改按钮'
  UNION ALL SELECT '课程删除',   @courseMenu, 4, 'main:course:remove', '课程删除按钮'
  UNION ALL SELECT '课程导出',   @courseMenu, 5, 'main:course:export', '课程导出按钮'
  UNION ALL SELECT '选课管理',   @courseMenu, 6, 'main:course:assign', '学生选课/退课管理'
) t
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.`parent_id` = t.pid AND m.`perms` = t.pm);

-- ========== 3. 角色授权 ==========
SET @roleStudent = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'student' AND `del_flag` = '0' LIMIT 1);
SET @roleTeacher = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'teacher' AND `del_flag` = '0' LIMIT 1);

-- 3.1 教师：课程管理（列表 + 查询 + 选课管理）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleTeacher, m.`menu_id` FROM `sys_menu` m
WHERE m.`perms` IN ('main:course:list', 'main:course:query', 'main:course:assign');

-- 3.2 学生：教学管理目录 + 课程管理（只读）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleStudent, m.`menu_id` FROM `sys_menu` m
WHERE m.`perms` = 'main:course:list';

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleStudent, @teachingDir FROM DUAL;

-- ========== 4. 校验 ==========
SELECT `menu_id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `component`, `order_num`
FROM `sys_menu` WHERE `parent_id` = @courseMenu OR `menu_id` = @courseMenu ORDER BY `parent_id`, `order_num`;

SELECT r.`role_key`, COUNT(rm.`menu_id`) AS menu_count
FROM `sys_role` r LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`role_id`
WHERE r.`role_key` IN ('student', 'teacher') GROUP BY r.`role_key`;
