-- ----------------------------
-- hms_clockin_record 增加用户昵称和部门字段
-- add by CYQ 2026-06-06 说明: 打卡记录增加用户昵称支持导出，增加部门支持查询和导出
-- ----------------------------
ALTER TABLE `hms_clockin_record`
ADD COLUMN `nick_name` varchar(64) DEFAULT '' COMMENT '用户昵称' AFTER `user_name`,
ADD COLUMN `dept_id` bigint DEFAULT NULL COMMENT '部门ID' AFTER `nick_name`,
ADD COLUMN `dept_name` varchar(64) DEFAULT '' COMMENT '部门名称' AFTER `dept_id`,
ADD INDEX `idx_dept_id` (`dept_id`) USING BTREE;
