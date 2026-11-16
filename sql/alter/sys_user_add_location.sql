-- ----------------------------
-- 用户当前所在城市：直接挂在 sys_user 上，供天气推送按实际城市取数
-- add by Qwen 2026-10-26 说明: 移动端启动时静默上报定位，写 sys_user.current_*
-- ----------------------------
ALTER TABLE `sys_user`
  ADD COLUMN `current_city` varchar(50) DEFAULT NULL COMMENT '当前所在城市' AFTER `wx_openid`,
  ADD COLUMN `current_district` varchar(50) DEFAULT NULL COMMENT '当前所在区县' AFTER `current_city`,
  ADD COLUMN `current_adcode` varchar(20) DEFAULT NULL COMMENT '当前城市行政区划编码（高德adcode）' AFTER `current_district`,
  ADD COLUMN `current_latitude` decimal(12,8) DEFAULT NULL COMMENT '当前纬度' AFTER `current_adcode`,
  ADD COLUMN `current_longitude` decimal(12,8) DEFAULT NULL COMMENT '当前经度' AFTER `current_latitude`,
  ADD COLUMN `location_time` datetime DEFAULT NULL COMMENT '位置上报时间' AFTER `current_longitude`;

-- ----------------------------
-- 清理上一版单独建的表（城市已并入 sys_user）
-- ----------------------------
DROP TABLE IF EXISTS `hms_user_location`;

-- ----------------------------
-- 配置项：天气推送取哪个用户的位置（默认 108，可在 系统管理-参数设置 中修改）
-- ----------------------------
DELETE FROM `sys_config` WHERE `config_key` = 'weather.location.userId';
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`) VALUES
('weather.location.userId', '108', 'Y', 'admin', sysdate(), '天气推送目标用户ID，取该用户 sys_user 上最近上报的城市；无上报时回退默认城市（厦门湖里区）');
