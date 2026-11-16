-- ----------------------------
-- 「教学管理」菜单 + 「学生/教师」角色注册
-- add by CYQ 2026-10-18 说明:
--   配合 hms_student 重建与 hms_teacher 新建，向 sys_menu 注册「教学管理」目录、
--   「学生管理」「教师管理」菜单及其功能按钮，并创建「学生」「教师」两个角色、
--   分配对应菜单权限（用于「生成账号」后自动授予角色）。
--   PC 端组件：main/student/index、main/teacher/index
--   幂等：可重复执行，不会产生重复菜单或重复角色。
-- ----------------------------

-- ========== 1. 教学管理目录 ==========
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '教学管理', 0, 6, 'teaching', '', '', '', 1, 0, 'M', '0', '0', '', 'education', 'admin', NOW(), '教学管理目录'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'teaching');

SET @teachingDir = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'teaching' LIMIT 1);

-- ========== 2. 学生管理菜单 ==========
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '学生管理', @teachingDir, 1, 'student', 'main/student/index', '', '', 1, 0, 'C', '0', '0',
       'main:student:list', 'user', 'admin', NOW(), '学生建档管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'student');

SET @studentMenu = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'student' LIMIT 1);

-- ========== 3. 教师管理菜单 ==========
INSERT INTO `sys_menu`
(`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '教师管理', @teachingDir, 2, 'teacher', 'main/teacher/index', '', '', 1, 0, 'C', '0', '0',
       'main:teacher:list', 'peoples', 'admin', NOW(), '教师建档管理'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'teacher');

SET @teacherMenu = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = @teachingDir AND `path` = 'teacher' LIMIT 1);

-- ========== 4. 学生管理功能按钮 ==========
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT t.nm, t.pid, t.ord, '', '', '', '', 1, 0, 'F', '0', '0', t.pm, '#', 'admin', NOW(), t.rm
FROM (
  SELECT '学生查询'   AS nm, @studentMenu AS pid, 1 AS ord, 'main:student:query'        AS pm, '学生查询按钮'   AS rm
  UNION ALL SELECT '学生新增',   @studentMenu, 2, 'main:student:add',   '学生新增按钮'
  UNION ALL SELECT '学生修改',   @studentMenu, 3, 'main:student:edit',  '学生修改按钮'
  UNION ALL SELECT '学生删除',   @studentMenu, 4, 'main:student:remove','学生删除按钮'
  UNION ALL SELECT '学生导出',   @studentMenu, 5, 'main:student:export','学生导出按钮'
  UNION ALL SELECT '生成账号',   @studentMenu, 6, 'main:student:generateUser', '根据学号生成系统账号'
) t
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.`parent_id` = t.pid AND m.`perms` = t.pm);

-- ========== 5. 教师管理功能按钮 ==========
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`,
 `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT t.nm, t.pid, t.ord, '', '', '', '', 1, 0, 'F', '0', '0', t.pm, '#', 'admin', NOW(), t.rm
FROM (
  SELECT '教师查询'   AS nm, @teacherMenu AS pid, 1 AS ord, 'main:teacher:query'        AS pm, '教师查询按钮'   AS rm
  UNION ALL SELECT '教师新增',   @teacherMenu, 2, 'main:teacher:add',   '教师新增按钮'
  UNION ALL SELECT '教师修改',   @teacherMenu, 3, 'main:teacher:edit',  '教师修改按钮'
  UNION ALL SELECT '教师删除',   @teacherMenu, 4, 'main:teacher:remove','教师删除按钮'
  UNION ALL SELECT '教师导出',   @teacherMenu, 5, 'main:teacher:export','教师导出按钮'
  UNION ALL SELECT '生成账号',   @teacherMenu, 6, 'main:teacher:generateUser', '根据工号生成系统账号'
) t
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` m WHERE m.`parent_id` = t.pid AND m.`perms` = t.pm);

-- ========== 6. 创建「学生」「教师」角色 ==========
INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`,
                        `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT '学生', 'student', 10, '1', 1, 1, '0', '0', 'admin', NOW(), '学生角色（生成账号时自动分配）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_key` = 'student' AND `del_flag` = '0');

INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`,
                        `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT '教师', 'teacher', 9, '1', 1, 1, '0', '0', 'admin', NOW(), '教师角色（生成账号时自动分配）'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_key` = 'teacher' AND `del_flag` = '0');

SET @roleStudent = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'student' AND `del_flag` = '0' LIMIT 1);
SET @roleTeacher = (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'teacher' AND `del_flag` = '0' LIMIT 1);

-- ========== 7. 角色菜单授权 ==========
-- 说明：菜单 ID 统一按 perms / path 反查，避免硬编码
-- 7.1 学生角色：成绩分析记录（查询/提交/导出）+ 资料库管理（查询/下载）+ 知识地图
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleStudent, m.`menu_id` FROM `sys_menu` m
WHERE m.`perms` IN ('main:scoreRecord:list', 'main:scoreRecord:query', 'main:scoreRecord:submit',
                    'main:scoreRecord:export', 'main:document:list', 'main:document:query',
                    'main:document:download', 'main:knowledgemap:list')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = @roleStudent AND rm.`menu_id` = m.`menu_id`);

-- 7.2 教师角色：教学管理目录 + 学生管理（除生成账号）+ 成绩分析记录 + 资料库管理 + 知识地图
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleTeacher, m.`menu_id` FROM `sys_menu` m
WHERE m.`perms` IN ('main:student:list', 'main:student:query', 'main:student:add', 'main:student:edit',
                    'main:student:export', 'main:scoreRecord:list', 'main:scoreRecord:query',
                    'main:scoreRecord:submit', 'main:scoreRecord:export', 'main:document:list',
                    'main:document:query', 'main:document:download', 'main:knowledgemap:list')
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = @roleTeacher AND rm.`menu_id` = m.`menu_id`);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @roleTeacher, m.`menu_id` FROM `sys_menu` m
WHERE m.`menu_id` = @teachingDir
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = @roleTeacher AND rm.`menu_id` = m.`menu_id`);

-- 7.3 超级管理员角色同步显示（admin 本身拥有 *:*:*，此处仅为角色管理页面的勾选展示）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.`menu_id` FROM `sys_menu` m
WHERE m.`parent_id` = @teachingDir
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = 1 AND rm.`menu_id` = m.`menu_id`);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, @teachingDir FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = 1 AND rm.`menu_id` = @teachingDir);

-- 7.4 补齐父级目录
-- add by CYQ 2026-10-18 说明: RuoYi 的菜单树要求父节点必须被授权，否则子菜单不会被下发到前端侧边栏。
--   扫描 3 层祖先目录（本项目菜单最深为 目录 → 菜单 → 按钮），用 INSERT IGNORE 借助主键去重。
--   兼容 MySQL 5.7（不使用 WITH RECURSIVE）。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, m.`parent_id`
FROM `sys_role_menu` rm JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` IN (@roleStudent, @roleTeacher) AND m.`parent_id` <> 0;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, m.`parent_id`
FROM `sys_role_menu` rm JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` IN (@roleStudent, @roleTeacher) AND m.`parent_id` <> 0;

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, m.`parent_id`
FROM `sys_role_menu` rm JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` IN (@roleStudent, @roleTeacher) AND m.`parent_id` <> 0;



-- ========== 8. 顶层菜单排序 ==========
-- add by CYQ 2026-10-18 说明: 「教学管理」放在「移动端管理」之下、「资料库」之上。
--   以「移动端管理」的 order_num 为基准顺延，避免与既有目录的序号冲突；
--   重复执行结果一致（幂等）。
SET @mobileOrder = (SELECT `order_num` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'mobile' LIMIT 1);

UPDATE `sys_menu` SET `order_num` = @mobileOrder + 1
WHERE `parent_id` = 0 AND `path` = 'teaching';

UPDATE `sys_menu` SET `order_num` = @mobileOrder + 2
WHERE `parent_id` = 0 AND `path` = 'main';

UPDATE `sys_menu` SET `order_num` = @mobileOrder + 3
WHERE `parent_id` = 0 AND `path` = 'finance';

-- ========== 9. 校验 ==========
SELECT `menu_id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `component`, `order_num`
FROM `sys_menu` WHERE `parent_id` = @teachingDir OR `menu_id` = @teachingDir ORDER BY `parent_id`, `order_num`;

SELECT `role_id`, `role_name`, `role_key` FROM `sys_role` WHERE `role_key` IN ('student', 'teacher');

SELECT r.`role_key`, COUNT(rm.`menu_id`) AS menu_count
FROM `sys_role` r LEFT JOIN `sys_role_menu` rm ON rm.`role_id` = r.`role_id`
WHERE r.`role_key` IN ('student', 'teacher') GROUP BY r.`role_key`;
