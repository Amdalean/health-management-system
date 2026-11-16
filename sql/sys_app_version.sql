-- ----------------------------
-- APP版本管理表
-- ----------------------------
DROP TABLE IF EXISTS `sys_app_version`;
CREATE TABLE `sys_app_version` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号 (如 1.2.0)',
  `version_code` int(11) DEFAULT NULL COMMENT '版本码 (如 12)',
  `platform` varchar(20) DEFAULT NULL COMMENT '平台 (android/ios)',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名称',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小(字节)',
  `force_update` tinyint(1) DEFAULT '0' COMMENT '是否强制更新 (0=否, 1=是)',
  `release_notes` text COMMENT '更新日志',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 (0=禁用, 1=启用)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='APP版本管理表';

-- ----------------------------
-- 初始化数据
-- ----------------------------
INSERT INTO `sys_app_version` VALUES (1, '1.2.0', 12, 'android', 'Caelum_v1.2.0.apk', '/profile/apk/20250419_Caelum_v1.2.0.apk', 52428800, 0, '1. 优化用户体验\n2. 修复已知问题', 1, 'admin', '2026-04-19 10:00:00', '', NULL, NULL);
