-- ----------------------------
-- 把「资料库」目录下的菜单合并到「教学管理」目录
-- add by CYQ 2026-10-18 说明:
--   1) 将「资料库」(path=main, menu_id=2025) 下的全部子菜单
--      （成绩分析记录 / 资料库管理 / 知识地图）迁移到「教学管理」(path=teaching) 下，
--      并重排为 4/5/6（排在 学生管理/教师管理/课程管理 之后）；
--   2) 为原本只授权了子菜单的角色补授「教学管理」目录，避免菜单树因缺少父节点而不下发；
--   3) 删除已清空的「资料库」目录及其角色授权记录。
--   幂等：可重复执行。
-- ----------------------------

SET @teachingDir = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'teaching' LIMIT 1);
SET @libraryDir  = (SELECT `menu_id` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'main' LIMIT 1);

-- ========== 1. 迁移子菜单 ==========
-- 一次性把资料库下所有节点挂到教学管理，避免遗漏隐藏菜单
UPDATE `sys_menu` SET `parent_id` = @teachingDir
WHERE `parent_id` = @libraryDir;

-- ========== 2. 重排顺序：学生(1) 教师(2) 课程(3) → 成绩分析(4) 资料库(5) 知识地图(6) ==========
UPDATE `sys_menu` SET `order_num` = 1 WHERE `parent_id` = @teachingDir AND `path` = 'student';
UPDATE `sys_menu` SET `order_num` = 2 WHERE `parent_id` = @teachingDir AND `path` = 'teacher';
UPDATE `sys_menu` SET `order_num` = 3 WHERE `parent_id` = @teachingDir AND `path` = 'course';
UPDATE `sys_menu` SET `order_num` = 4 WHERE `parent_id` = @teachingDir AND `path` = 'scoreRecord';
UPDATE `sys_menu` SET `order_num` = 5 WHERE `parent_id` = @teachingDir AND `path` = 'document';
UPDATE `sys_menu` SET `order_num` = 6 WHERE `parent_id` = @teachingDir AND `path` = 'knowledgemap';

-- ========== 3. 补授父级目录 ==========
-- 凡是拥有「教学管理」下任一子菜单的角色，都必须同时拥有「教学管理」目录本身
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.`role_id`, @teachingDir
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE m.`parent_id` = @teachingDir;

-- ========== 4. 删除已清空的资料库目录 ==========
DELETE FROM `sys_role_menu` WHERE `menu_id` = @libraryDir;

SET @libraryChildCount = (SELECT COUNT(*) FROM `sys_menu` WHERE `parent_id` = @libraryDir);
DELETE FROM `sys_menu` WHERE `menu_id` = @libraryDir AND @libraryChildCount = 0;

-- ========== 5. 顶层菜单序号收敛（消除资料库留下空位） ==========
SET @mobileOrder = (SELECT `order_num` FROM `sys_menu` WHERE `parent_id` = 0 AND `path` = 'mobile' LIMIT 1);

UPDATE `sys_menu` SET `order_num` = @mobileOrder + 1 WHERE `parent_id` = 0 AND `path` = 'teaching';
UPDATE `sys_menu` SET `order_num` = @mobileOrder + 2 WHERE `parent_id` = 0 AND `path` = 'finance';

-- ========== 6. 校验 ==========
SELECT `menu_id`, `menu_name`, `parent_id`, `menu_type`, `perms`, `component`, `order_num`
FROM `sys_menu` WHERE `parent_id` = @teachingDir ORDER BY `order_num`;

SELECT `order_num`, `menu_id`, `menu_name` FROM `sys_menu` WHERE `parent_id` = 0 ORDER BY `order_num`;
