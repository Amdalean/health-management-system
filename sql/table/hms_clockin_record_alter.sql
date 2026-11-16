-- ----------------------------
-- hms_clockin_record 增加打卡类型字段
-- add by CYQ 2026-06-06 说明: 打卡时自动区分内勤/外勤
-- ----------------------------
ALTER TABLE `hms_clockin_record`
ADD COLUMN `clockin_type` varchar(10) DEFAULT '' COMMENT '打卡类型 (内勤/外勤)' AFTER `address`;
