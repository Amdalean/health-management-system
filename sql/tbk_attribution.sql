-- ----------------------------
-- 链接台账 + 订单返现字段（现阶段只记录，不实际打款）
-- add by CYQ 2026-11-09
--
-- 背景：拼多多 custom_parameters 需要「pid + custom_parameters」逐组备案，
--       未备案时无法从订单反解用户。这里先记录每次生成的推广链接，
--       订单同步后按「平台 + 商品 + 时间窗」做近似归属，并记录应返现金额。
-- ----------------------------

CREATE TABLE IF NOT EXISTS `tbk_link` (
  `id`          bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint(20)    DEFAULT NULL COMMENT 'HMS用户ID',
  `anon_key`    varchar(64)   DEFAULT NULL COMMENT '匿名标识',
  `platform`    varchar(16)   NOT NULL COMMENT '平台：taobao/pdd',
  `item_id`     varchar(64)   DEFAULT NULL COMMENT '商品ID',
  `goods_sign`  varchar(80)   DEFAULT NULL COMMENT '拼多多 goods_sign',
  `pid`         varchar(64)   DEFAULT NULL COMMENT '推广位',
  `title`       varchar(500)  DEFAULT NULL COMMENT '商品标题',
  `short_url`   varchar(1000) DEFAULT NULL COMMENT '推广链接',
  `tpwd`        varchar(100)  DEFAULT NULL COMMENT '淘口令',
  `source`      varchar(16)   DEFAULT NULL COMMENT '来源：convert/pick',
  `create_time` datetime      DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_tbk_link_match` (`platform`, `item_id`, `create_time`),
  KEY `idx_tbk_link_sign` (`goods_sign`, `create_time`),
  KEY `idx_tbk_link_user` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推广链接台账';

-- 订单返现字段（可重复执行）
SET @has_goods_sign = (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'goods_sign');
SET @sql = IF(@has_goods_sign = 0,
  'ALTER TABLE tbk_order ADD COLUMN goods_sign varchar(80) DEFAULT NULL COMMENT ''拼多多 goods_sign'' AFTER item_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_link_id = (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'link_id');
SET @sql = IF(@has_link_id = 0,
  'ALTER TABLE tbk_order ADD COLUMN link_id bigint(20) DEFAULT NULL COMMENT ''关联链接台账ID'' AFTER external_user',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_match_type = (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'match_type');
SET @sql = IF(@has_match_type = 0,
  'ALTER TABLE tbk_order ADD COLUMN match_type varchar(16) DEFAULT NULL COMMENT ''归因方式：deterministic/approx/ambiguous/none'' AFTER link_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_cashback_rate = (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'cashback_rate');
SET @sql = IF(@has_cashback_rate = 0,
  'ALTER TABLE tbk_order ADD COLUMN cashback_rate decimal(5,2) DEFAULT NULL COMMENT ''返现比例（占佣金百分比）'' AFTER settled_commission',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_cashback_amount = (SELECT COUNT(*) FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'cashback_amount');
SET @sql = IF(@has_cashback_amount = 0,
  'ALTER TABLE tbk_order ADD COLUMN cashback_amount decimal(12,2) DEFAULT NULL COMMENT ''应返现金额'' AFTER cashback_rate',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_cashback_status = (SELECT COUNT(*) FROM information_schema.columns
                            WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'cashback_status');
SET @sql = IF(@has_cashback_status = 0,
  'ALTER TABLE tbk_order ADD COLUMN cashback_status varchar(16) DEFAULT NULL COMMENT ''返现状态：estimated/settled/invalid'' AFTER cashback_amount',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
