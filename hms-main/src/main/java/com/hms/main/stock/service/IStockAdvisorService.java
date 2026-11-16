package com.hms.main.stock.service;

import com.hms.main.stock.domain.KlineDataVO;
import com.hms.main.stock.domain.PerformancePointVO;
import com.hms.main.stock.domain.StockHoldingVO;
import com.hms.main.stock.domain.StockRecommendationVO;
import com.hms.main.stock.domain.StockScanResultVO;
import com.hms.main.stock.domain.StockTradeLogVO;
import java.util.List;
import java.util.Map;

/**
 * 股票顾问服务接口
 * add by CYQ 2026-06-05
 */
public interface IStockAdvisorService {

    /** 获取持仓概览（含策略、市值汇总） */
    Map<String, Object> getOverview();

    /** 获取持仓列表（含分配策略） */
    List<StockHoldingVO> getHoldings();

    /** 获取最新操作建议 */
    List<StockRecommendationVO> getRecommendations();

    /**
     * 分页获取操作建议
     * @param orderByColumn 排序字段（前端 prop，服务端白名单校验，非法值回退 createdTime）
     * @param isAsc asc/ascending 为升序，其余为降序
     * @return rows: 当前页数据, total: 总条数
     * add by CYQ 2026-08-04 操作建议分页
     * add by CYQ 2026-10-20 支持表格排序
     */
    Map<String, Object> getRecommendations(int pageNum, int pageSize,
                                           String orderByColumn, String isAsc);

    /** 兼容旧调用：不排序，默认按创建时间降序。 */
    default Map<String, Object> getRecommendations(int pageNum, int pageSize) {
        return getRecommendations(pageNum, pageSize, null, null);
    }

    /** 执行一次分析 */
    String runAnalysis();

    /** 内部调度：指定 user_id 运行分析（null=全部用户，admin/Quartz 场景）。add by CYQ 2026-08-18 */
    String runAnalysisForUser(Long userId);

    /** 执行全市场扫描 */
    String runScanner();

    /** 内部调度：指定 user_id 运行扫描（null=全部用户，admin/Quartz 场景）。add by CYQ 2026-08-18 */
    String runScannerForUser(Long userId);

    /**
     * 内部调度：多用户一次扫描（全市场分析只做一遍，Python 侧按用户分别分配/落库）。
     * 单用户时自动退回 runScannerForUser，保持行为一致。
     * add by CYQ 2026-10-14 跨用户扫描去重
     */
    String runScannerForUsers(List<Long> userIds);

    /**
     * 执行一次 A 股因子评估（Quartz 定时任务专用；内部调 Python main.py factor-eval）。
     * 单并发保护：已有 running 且心跳 &lt;30 分钟时拒绝重复启动。
     * add by CYQ 2026-11-13（P2a）
     */
    String runFactorEval();

    /**
     * 标记建议已执行。可传入实际成交单价与股数；
     * 任一为空时回退到建议值（price_hint / suggested_shares）。
     * add by CYQ 2026-08-04 实际执行
     */
    void markExecuted(Long recommendationId, Double actualPrice, Integer actualShares);

    /** 标记建议已过期（不执行交易） */
    void markExpired(Long recommendationId);

    /** 获取最新扫描结果 */
    List<StockScanResultVO> getScanResults();

    /** 同步日线行情到 MySQL */
    String runSyncMarketData();

    /**
     * 同步季频财务到 MySQL（Baostock → stock_financial_quarter，全局数据无用户隔离）
     * add by CYQ 2026-09-06 财务层闭环
     */
    String runSyncFinancial();

    /**
     * 获取指定标的已披露季频财务（按 pub_date<=今日取最新 stat_date 起近8期，PIT 口径展示）
     * add by CYQ 2026-09-06 财务层闭环
     */
    List<com.hms.main.stock.domain.StockFinancialVO> getFinancials(String symbol);

    /** 获取交易记录 */
    List<StockTradeLogVO> getTradeLog();

    /** 获取组合表现（权益曲线） */
    List<PerformancePointVO> getPortfolioPerformance();

    /** 获取指定标的 K 线数据（含MA信号） */
    KlineDataVO getKlineData(String symbol, String year);

    /** 获取系统运行状态（数据同步、Tushare连通、邮件配置等） */
    Map<String, Object> getSystemStatus();

    /**
     * 查询未通知（pending 且 notified=0）的信号并发送邮件通知；发送后置 notified=1，
     * 保证每条信号只发一次。
     * <p>
     * 注意与 processPendingStopSignals 的区别：后者统计的是"当前 pending 的 SELL/REDUCE 总数"
     * （状态计数，含已通知的），本方法统计的是"从未通知过"的增量。两者数字不同是正常的。
     *
     * @param source 来源标识（如"持仓分析"、"全市场扫描"）
     * @return 可读结果，如 "已发送 2 条邮件（命中 3 条未通知；1 条因无邮箱跳过）"
     * add by CYQ 2026-07-06；update by CYQ 2026-10-14 返回可读报告（原返回 int 易与 pending 计数混读）
     */
    String sendSignalNotification(String source);

    /**
     * 新增持仓（写入后立即分配策略，与策略仓同权纳管）
     * add by CYQ 2026-07-06 持仓管理；update by CYQ 2026-10-27 建仓即分配策略
     */
    void addHolding(String symbol, String name, int shares, double costPrice);

    /**
     * 更新持仓
     * add by CYQ 2026-07-06 持仓管理
     */
    void updateHolding(String symbol, Integer shares, Double costPrice);

    /**
     * 删除持仓
     * add by CYQ 2026-07-06 持仓管理
     */
    void deleteHolding(String symbol);

    /**
     * 获取孤儿持仓列表（有持仓但无 active 策略记录的标的）
     * add by CYQ 2026-07-07 (task t_456ebc43)
     */
    List<Map<String, Object>> getOrphanHoldings();

    /**
     * 处理待执行的止损/减仓信号:
     * 1) 自动过期超 N 天的 pending 信号
     * 2) 查询 pending SELL/REDUCE 信号，发送通知
     * 3) 硬止损(-25%+)信号提升通知等级为紧急
     *
     * @return 处理报告
     * add by CYQ 2026-07-16 (task t_5232f6f2)
     */
    String processPendingStopSignals();

    /**
     * 为孤儿持仓分配策略（触发 Python assign_orphans）
     * add by CYQ 2026-07-07 (task t_456ebc43)
     */
    String assignOrphanStrategies();

    /**
     * 回测任务状态（读 sys_job_log 最近一次，回测在定时任务管理里手动执行）
     * add by CYQ 2026-09-06
     */
    Map<String, Object> getBacktestJobStatus();

    /** 因子任务状态（最新 run / 运行中 / 数据新鲜度 / 结论计数）。add by CYQ 2026-11-13 */
    Map<String, Object> getFactorStatus();

    /** 因子结论列表（runId 为空取最新）。add by CYQ 2026-11-13 */
    List<Map<String, Object>> getFactorEvals(String runId);

    /** 因子报告载荷（kind 默认 summary）。add by CYQ 2026-11-13 */
    Map<String, Object> getFactorReport(String runId, String kind);

    /** 因子运行历史（最近 N 次）。add by CYQ 2026-11-13 */
    List<Map<String, Object>> getFactorRuns(int limit);

    /**
     * 组合级回测结果（共享现金账户）；runId 为空取用户域内最新一次运行。
     * add by CYQ 2026-09-06
     */
    Map<String, Object> getPortfolioBacktest(Long runId);

    /**
     * 组合级回测历史运行批次列表（供前台切换对比；Python 侧每用户保留最近 10 次）
     * add by CYQ 2026-09-06
     */
    List<Map<String, Object>> listPortfolioRuns();

    /**
     * 指定批次的组合逐笔成交（runId 为空则取最新一次）
     * add by CYQ 2026-09-06
     */
    List<com.hms.main.stock.domain.StockBacktestTradeVO> getPortfolioTrades(Long runId);

    /**
     * 触发组合级回测（Python main.py portfolio，同步，Quartz 线程内调用）
     * add by CYQ 2026-09-06
     */
    String runPortfolioBacktestForUser(Long userId);

    /**
     * 触发组合级回测（指定年份，空走 config 默认年；每年独立 run_id 落库）
     * add by CYQ 2026-09-10
     */
    String runPortfolioBacktestForUser(Long userId, Integer year);

    /**
     * 触发组合级回测（连贯区间：一次取全、单次撮合、单个 run_id 落库）
     * add by CYQ 2026-09-10
     */
    String runPortfolioBacktestForUser(Long userId, Integer startYear, Integer endYear);

    /**
     * 同 {@link #runPortfolioBacktestForUser(Long, Integer, Integer)}，另传初始资金（null=走 config）。
     * add by CYQ 2026-09-24 初始资金
     */
    String runPortfolioBacktestForUser(Long userId, Integer startYear, Integer endYear, Double capital);

    /**
     * 持仓顾问生效参数：读 veighNa/config.json 的 portfolio/cost_model/risk/valuation/
     * sell_discipline/evaluation/benchmark 段，供前端「顾问参数」面板展示。
     * add by CYQ 2026-09-24 顾问参数
     */
    Map<String, Object> getAdvisorParams();

    /**
     * 搜索已下载的股票标的（基于 stock_daily_quote 去重 + stock_basic_cache 名称）
     * 用于 K线页可查询非持仓但已下载行情的股票，避免下拉框滥用
     * add by CYQ 2026-08-31
     */
    List<Map<String, Object>> searchDownloadedSymbols(String keyword, int limit);

    /**
     * 股票代码/名称互查（全 A 股 stock_basic_cache），返回 [{symbol:6位, name}]；
     * 精确匹配排前。用于新增持仓时输入一项自动带出另一项。
     * add by CYQ 2026-10-08
     */
    List<Map<String, Object>> lookupSymbols(String keyword, int limit);

    /**
     * 手工调整现金（入金/出金）。amount 正数=入金、负数=出金；出金后余额不得为负。
     * 落一条 account_cash 流水，返回 {balance, changeAmount, changeType}。
     * add by CYQ 2026-09-10 现金管理
     */
    Map<String, Object> adjustCash(java.math.BigDecimal amount, String note);

    /**
     * 现金流水（近 N 条，含入/出金与成交引起的变动）。
     * add by CYQ 2026-09-10 现金管理
     */
    List<Map<String, Object>> getCashLog(int limit);
}
