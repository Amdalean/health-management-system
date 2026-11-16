-- 操作建议表添加股票名称字段
-- add by CYQ 2026年7月3日
ALTER TABLE stock_recommendation
    ADD COLUMN name VARCHAR(50) COMMENT '股票名称' AFTER symbol;

-- 回填已有数据（从 stock_holding 关联）
UPDATE stock_recommendation r
    LEFT JOIN stock_holding h ON r.symbol = h.symbol
SET r.name = h.name
WHERE r.name IS NULL;
