-- ----------------------------
-- Table structure for hms_office_location
-- add by CYQ 2026-06-06 说明: 办公地点配置表，支持多办公地点，打卡时做地理围栏判定
-- ----------------------------
DROP TABLE IF EXISTS `hms_office_location`;
CREATE TABLE `hms_office_location` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(100) DEFAULT '' COMMENT '办公地点名称',
  `latitude` decimal(12,8) DEFAULT NULL COMMENT '纬度',
  `longitude` decimal(12,8) DEFAULT NULL COMMENT '经度',
  `radius` int DEFAULT '500' COMMENT '有效半径(米)',
  `status` char(1) DEFAULT '1' COMMENT '状态 (0=停用 1=启用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='办公地点配置表';

-- ----------------------------
-- 初始数据：默认办公地点
-- ----------------------------
INSERT INTO `hms_office_location` (`name`, `latitude`, `longitude`, `radius`, `status`, `create_by`, `create_time`) VALUES
('默认办公点', 39.904200, 116.407396, 500, '1', 'admin', sysdate());
