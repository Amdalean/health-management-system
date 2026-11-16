-- ----------------------------
-- 参数配置：移动端打卡-启用内勤外勤区分
-- add by CYQ 2026-08-13 说明: 关闭后移动端打卡不再校验内勤外勤（不判定打卡类型、不弹外勤确认）
-- 配置位置：PC 端「系统管理 → 参数设置」
-- ----------------------------
INSERT INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `remark`) VALUES
('移动端打卡-启用内勤外勤区分', 'mobile.clockin.enableOfficeType', 'true', 'Y', 'admin', sysdate(), '是否校验内勤外勤（true校验，false不校验）');
