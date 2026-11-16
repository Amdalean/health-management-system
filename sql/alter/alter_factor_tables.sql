-- ============================================================
-- 因子层建表 / 加列（路线 1，P0）—— 父仓库执行副本
-- 源文件: veighNa/sql/init_factor_tables.sql（以源文件为准，本文件仅便于运维执行）
-- 数据库: ry（与 HMS 现有库同库）
-- 幂等：可重复执行；不删除、不改既有列语义
-- add by CYQ 2026-11-12
--
-- 说明：
--   1. 本脚本只加列/建表，不动既有数据；生产执行前先在从库演练。
--   2. CI 不会自动执行 SQL，需运维手工执行（见 docs/FACTOR_MINING_PLAN.md §4.5 C10）。
--   3. 四张新表都是全局研究数据，**不带 user_id**，不要套用 Java userScope()。
-- ============================================================

-- 1. 行情表补三列（涨跌停 / ST 判定所需）
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE stock_daily_quote ADD COLUMN pre_close DECIMAL(10,4) NULL COMMENT ''昨收(未复权)'' AFTER close',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'stock_daily_quote' AND column_name = 'pre_close');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE stock_daily_quote ADD COLUMN pct_chg DECIMAL(10,4) NULL COMMENT ''涨跌幅(%)'' AFTER pre_close',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'stock_daily_quote' AND column_name = 'pct_chg');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE stock_daily_quote ADD COLUMN is_st TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''当日是否ST/退市整理''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'stock_daily_quote' AND column_name = 'is_st');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 2. 每日基础指标（规模/换手/估值）
CREATE TABLE IF NOT EXISTS stock_daily_basic (
    symbol          VARCHAR(10)   NOT NULL COMMENT '股票代码',
    trade_date      DATE          NOT NULL,
    turnover_rate   DECIMAL(10,4) NULL COMMENT '换手率%(流通)',
    turnover_rate_f DECIMAL(10,4) NULL COMMENT '换手率%(自由流通)',
    total_share     DECIMAL(20,2) NULL COMMENT '总股本(万股)',
    float_share     DECIMAL(20,2) NULL COMMENT '流通股本(万股)',
    free_share      DECIMAL(20,2) NULL COMMENT '自由流通股本(万股)',
    total_mv        DECIMAL(20,2) NULL COMMENT '总市值(万元)',
    circ_mv         DECIMAL(20,2) NULL COMMENT '流通市值(万元)',
    pe_ttm          DECIMAL(12,4) NULL,
    pb              DECIMAL(12,4) NULL,
    ps_ttm          DECIMAL(12,4) NULL,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (symbol, trade_date),
    KEY idx_date (trade_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日基础指标(Tushare daily_basic)';

-- 3. 因子任务运行状态（前台状态条 / 进度 / 历史）
CREATE TABLE IF NOT EXISTS stock_factor_run (
    run_id        VARCHAR(40)   NOT NULL COMMENT 'yyyyMMddHHmmss-xxxx',
    trigger_type  VARCHAR(16)   NOT NULL DEFAULT 'cli' COMMENT 'job/manual/cli',
    stage         VARCHAR(32)   NULL COMMENT 'data_check/factor_calc/evaluate/report/done',
    status        VARCHAR(16)   NOT NULL DEFAULT 'queued' COMMENT 'queued/running/success/failed',
    progress      TINYINT       NOT NULL DEFAULT 0 COMMENT '0-100',
    start_time    DATETIME      NULL,
    end_time      DATETIME      NULL,
    heartbeat     DATETIME      NULL COMMENT 'Python 每阶段更新，前台判活',
    data_max_date DATE          NULL,
    git_head      VARCHAR(40)   NULL,
    config_hash   VARCHAR(40)   NULL,
    error_msg     VARCHAR(1000) NULL,
    PRIMARY KEY (run_id),
    KEY idx_status (status, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='因子任务运行状态';

-- 4. 因子检验结论（只存结论，不存因子值）
CREATE TABLE IF NOT EXISTS stock_factor_eval (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id        VARCHAR(40)   NOT NULL COMMENT '一次评估批次',
    factor_name   VARCHAR(40)   NOT NULL,
    universe      VARCHAR(40)   NOT NULL COMMENT '检验池标识',
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    freq          VARCHAR(10)   NOT NULL DEFAULT 'daily',
    n_days        INT           NOT NULL,
    coverage      DECIMAL(8,4)  NULL COMMENT '有效截面覆盖率均值',
    ic_mean       DECIMAL(10,6) NULL,
    ic_std        DECIMAL(10,6) NULL,
    icir          DECIMAL(10,6) NULL COMMENT 'IC均值/标准差',
    ic_win_rate   DECIMAL(8,4)  NULL,
    ic_t_stat     DECIMAL(10,4) NULL,
    fdr_p         DECIMAL(10,6) NULL COMMENT 'BH-FDR 校正后 p 值',
    ls_annual     DECIMAL(10,6) NULL COMMENT '多空年化',
    ls_sharpe     DECIMAL(10,4) NULL,
    monotonicity  DECIMAL(6,4)  NULL COMMENT '5分层收益单调性(秩相关)',
    turnover      DECIMAL(10,4) NULL COMMENT '分层组合日均换手',
    verdict       VARCHAR(16)   NOT NULL COMMENT 'PASS/FAIL/INSUFFICIENT',
    reasons_json  TEXT          NULL,
    data_max_date DATE          NULL COMMENT '本次评估实际用到的行情最大日期',
    config_hash   VARCHAR(40)   NULL COMMENT 'factors 配置指纹',
    git_head      VARCHAR(40)   NULL COMMENT '运行时代码版本',
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_run (run_id),
    KEY idx_factor (factor_name, start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='因子检验结论';

-- 5. 报告载荷（图表/回测数据，前端原样消费）
CREATE TABLE IF NOT EXISTS stock_factor_report (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id        VARCHAR(40)   NOT NULL,
    kind          VARCHAR(32)   NOT NULL COMMENT 'summary/ic_series/layered_nav/composite/backtest',
    payload_json  LONGTEXT      NOT NULL,
    created_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_run_kind (run_id, kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='因子报告载荷';

-- 6. 复核（人工执行后检查）
-- SELECT COUNT(*) FROM information_schema.columns
--  WHERE table_schema=DATABASE() AND table_name='stock_daily_quote'
--    AND column_name IN ('pre_close','pct_chg','is_st');
-- 期望 3
