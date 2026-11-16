-- 操作建议表添加"邮件通知已发送"标志，根治 sendSignalNotification 依赖 5 分钟时间窗口导致漏发的问题
-- add by CYQ 2026年9月22日
ALTER TABLE stock_recommendation
    ADD COLUMN notified TINYINT(1) NOT NULL DEFAULT 0 COMMENT '邮件通知是否已发送: 0未发 1已发' AFTER status;

-- 通知查询条件索引
ALTER TABLE stock_recommendation
    ADD INDEX idx_notified (status, notified);
