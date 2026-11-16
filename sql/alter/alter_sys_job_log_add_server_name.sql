-- ----------------------------
-- 调度日志增加执行服务器字段
-- add by CYQ 2026-09-14 说明: sys_job_log 增加 server_name 记录执行任务所属服务器
-- ----------------------------
ALTER TABLE `sys_job_log`
  ADD COLUMN `server_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '执行服务器' AFTER `invoke_target`;
