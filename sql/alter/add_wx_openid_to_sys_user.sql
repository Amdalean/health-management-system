-- sys_user 增加微信小程序 openid 字段（微信一键登录绑定用）
-- add by CYQ 2026-09-06 微信小程序一键登录：openid 记录到用户表
ALTER TABLE sys_user ADD COLUMN wx_openid VARCHAR(64) NULL DEFAULT NULL COMMENT '微信小程序openid' AFTER remark;
CREATE UNIQUE INDEX idx_sys_user_wx_openid ON sys_user(wx_openid);
