/*
 Navicat Premium Data Transfer

 Date: 2026-06-05 22:30:00
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for hms_clockin_record
-- ----------------------------
DROP TABLE IF EXISTS `hms_clockin_record`;
CREATE TABLE `hms_clockin_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `user_name` varchar(64) DEFAULT '' COMMENT '用户名称',
  `nick_name` varchar(64) DEFAULT '' COMMENT '用户昵称',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `dept_name` varchar(64) DEFAULT '' COMMENT '部门名称',
  `latitude` decimal(12,8) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(12,8) DEFAULT NULL COMMENT '经度',
  `address` varchar(500) DEFAULT '' COMMENT '打卡地址',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id` (`user_id`) USING BTREE,
  INDEX `idx_create_time` (`create_time`) USING BTREE,
  INDEX `idx_dept_id` (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='打卡记录表';

SET FOREIGN_KEY_CHECKS = 1;
