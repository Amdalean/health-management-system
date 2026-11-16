-- ----------------------------
-- tbk_order 增加用户归因字段（返现基础）
-- add by CYQ 2026-11-09
--
-- 说明：
--   user_id        HMS 用户ID，登录态下由后端从 token 解析，匿名请求为空；
--   external_user  平台侧归因标识（拼多多 custom_parameters.uid / 淘宝 special_id / 闪购 sid）。
--   user_name 不落库，查询时 left join sys_user 取。
-- ----------------------------

SET @has_user_id = (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'user_id');
SET @sql = IF(@has_user_id = 0,
  'ALTER TABLE tbk_order ADD COLUMN user_id bigint(20) DEFAULT NULL COMMENT ''HMS用户ID'' AFTER pid',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_external_user = (SELECT COUNT(*) FROM information_schema.columns
                          WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND column_name = 'external_user');
SET @sql = IF(@has_external_user = 0,
  'ALTER TABLE tbk_order ADD COLUMN external_user varchar(64) DEFAULT NULL COMMENT ''平台归因标识'' AFTER user_id',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_index = (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE() AND table_name = 'tbk_order' AND index_name = 'idx_tbk_order_user');
SET @sql = IF(@has_index = 0,
  'ALTER TABLE tbk_order ADD KEY idx_tbk_order_user (user_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 执行后检查
-- SHOW COLUMNS FROM tbk_order LIKE 'user%';
-- SHOW COLUMNS FROM tbk_order LIKE 'external_user';
