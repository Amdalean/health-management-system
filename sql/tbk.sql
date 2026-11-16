-- ----------------------------
-- 淘宝客转链流水表
-- add by CYQ 2026-11-02 淘宝客转链模块
-- ----------------------------
CREATE TABLE IF NOT EXISTS `tbk_convert_log` (
  `id`          bigint(20)   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content`     varchar(500) DEFAULT NULL COMMENT '用户原始输入',
  `item_id`     varchar(32)  DEFAULT NULL COMMENT '商品ID',
  `title`       varchar(500) DEFAULT NULL COMMENT '商品标题',
  `provider`    varchar(32)  DEFAULT NULL COMMENT '转链通道',
  `ip`          varchar(64)  DEFAULT NULL COMMENT '请求IP',
  `status`      char(1)      DEFAULT '0' COMMENT '状态：0成功 1失败',
  `error_msg`   varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_time` datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tbk_convert_log_time` (`create_time`),
  KEY `idx_tbk_convert_log_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='淘宝客转链流水';
