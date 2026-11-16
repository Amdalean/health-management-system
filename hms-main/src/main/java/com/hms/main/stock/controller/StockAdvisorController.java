package com.hms.main.stock.controller;

import com.hms.common.annotation.Log;
import com.hms.common.core.controller.BaseController;
import com.hms.common.core.domain.AjaxResult;
import com.hms.common.core.page.TableDataInfo;
import com.hms.common.enums.BusinessType;
import com.hms.main.stock.service.IStockAdvisorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 股票持仓顾问 Controller
 * add by CYQ 2026-06-05
 */
@RestController
@RequestMapping("/main/stock")
public class StockAdvisorController extends BaseController {

    @Autowired
    private IStockAdvisorService stockAdvisorService;

    /** 持仓概览 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return success(stockAdvisorService.getOverview());
    }

    /** 持仓列表 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/holdings")
    public AjaxResult holdings() {
        return success(stockAdvisorService.getHoldings());
    }

    /** 操作建议列表（分页，支持排序） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/recommendations")
    public TableDataInfo recommendations(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "20") Integer pageSize,
                                         @RequestParam(required = false) String orderByColumn,
                                         @RequestParam(required = false) String isAsc) {
        Map<String, Object> result = stockAdvisorService.getRecommendations(
            pageNum, pageSize, orderByColumn, isAsc);
        return new TableDataInfo((List<?>) result.get("rows"), ((Number) result.get("total")).intValue());
    }

    /** 手动触发分析 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @PostMapping("/analyze")
    public AjaxResult analyze() {
        String result = stockAdvisorService.runAnalysis();
        return success(result);
    }

    /** 手动触发全市场扫描 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @PostMapping("/scanner")
    public AjaxResult scanner() {
        String result = stockAdvisorService.runScanner();
        return success(result);
    }

    /** 手动触发行情数据同步 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @Log(title = "股票顾问", businessType = BusinessType.OTHER)
    @PostMapping("/syncMarketData")
    public AjaxResult syncMarketData() {
        String result = stockAdvisorService.runSyncMarketData();
        return success(result);
    }

    /** 获取最新扫描结果 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/scanResults")
    public AjaxResult scanResults() {
        return success(stockAdvisorService.getScanResults());
    }

    /** 手动触发财务数据同步（Baostock 季频，add by CYQ 2026-09-06） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @Log(title = "股票顾问", businessType = BusinessType.OTHER)
    @PostMapping("/syncFinancial")
    public AjaxResult syncFinancial() {
        String result = stockAdvisorService.runSyncFinancial();
        return success(result);
    }

    /** 指定标的已披露季频财务（PIT 口径近8期，add by CYQ 2026-09-06） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/financial/{symbol}")
    public AjaxResult financial(@PathVariable String symbol) {
        return success(stockAdvisorService.getFinancials(symbol));
    }

    /** 持仓顾问生效参数（读 veighNa/config.json，与 Python 顾问同源） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/advisorParams")
    public AjaxResult advisorParams() {
        return success(stockAdvisorService.getAdvisorParams());
    }

    /** 手工调整现金：正数入金、负数出金（出金后余额不得为负） */
    @PreAuthorize("@ss.hasPermi('main:stock:edit')")
    @Log(title = "现金管理", businessType = BusinessType.UPDATE)
    @PostMapping("/cash/adjust")
    public AjaxResult adjustCash(@RequestParam java.math.BigDecimal amount,
                                 @RequestParam(required = false) String note) {
        return success(stockAdvisorService.adjustCash(amount, note));
    }

    /** 现金流水（近 N 条，含入/出金与成交引起的变动） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/cash/log")
    public AjaxResult cashLog(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        return success(stockAdvisorService.getCashLog(limit == null ? 50 : limit));
    }

    /** 标记建议已执行（可传实际成交单价 price、实际股数 shares，缺省回退到建议值） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @PutMapping("/execute/{id}")
    public AjaxResult execute(@PathVariable Long id,
                              @RequestParam(required = false) Double price,
                              @RequestParam(required = false) Integer shares) {
        stockAdvisorService.markExecuted(id, price, shares);
        return success();
    }

    /** 获取待处理建议列表（仅 pending） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/recommendations/pending")
    public AjaxResult pendingRecommendations() {
        return success(stockAdvisorService.getRecommendations()
            .stream()
            .filter(r -> "pending".equals(r.getStatus()))
            .collect(java.util.stream.Collectors.toList()));
    }

    /** 标记建议已过期（不执行交易） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @PutMapping("/expire/{id}")
    public AjaxResult expire(@PathVariable Long id) {
        stockAdvisorService.markExpired(id);
        return success();
    }

    /** MD 文档（Swagger） */
    @GetMapping("/md")
    public String md() {
        return "## 股票持仓顾问接口\n\n" +
            "- `GET /main/stock/overview` — 持仓概览\n" +
            "- `GET /main/stock/holdings` — 持仓列表\n" +
            "- `GET /main/stock/recommendations?pageNum=&pageSize=&orderByColumn=&isAsc=` — 操作建议（分页，返回 total/rows；支持排序）\n" +
            "- `POST /main/stock/analyze` — 触发持仓分析\n" +
            "- `POST /main/stock/scanner` — 触发全市场扫描\n" +
            "- `GET /main/stock/scanResults` — 获取扫描结果\n" +
            "- `PUT /main/stock/execute/{id}?price=&shares=` — 标记建议已执行（可传实际成交单价/股数）\n" +
            "- `GET /main/stock/tradeLog` — 交易记录\n" +
            "- `GET /main/stock/performance` — 组合权益曲线（按交易流水重建，累计收益率=时间加权，已剔除出入金）\n" +
            "- `GET /main/stock/kline/{symbol}?year=2025` — K线数据（含MA信号）\n" +
            "- `GET /main/stock/backtest/status` — 回测任务状态（读任务日志，回测在定时任务管理手动执行）\n" +
            "- `GET /main/stock/backtestPortfolio` — 组合级回测结果（共享现金账户，含净值曲线与期末持仓）\n" +
            "- `GET /main/stock/backtestPortfolio/runs` — 组合级回测历史批次列表\n" +
            "- `GET /main/stock/backtestPortfolio/trades?runId=` — 指定批次的组合逐笔成交（缺省取最新）\n" +
            "- `GET /main/stock/systemStatus` — 系统运行状态\n" +
            "- `GET /main/stock/recommendations/pending` — 待处理建议列表\n" +
            "- `PUT /main/stock/expire/{id}` — 标记建议过期\n" +
            "- `GET /main/stock/orphanHoldings` — 孤儿持仓（有持仓无策略）\n" +
            "- `POST /main/stock/assignOrphanStrategies` — 触发孤儿策略分配\n" +
            "- `GET /main/stock/symbols/search?q=&limit=20` — 搜索已下载标的（支持非持仓，防下拉滥用）\n" +
            "- `POST /main/stock/syncFinancial` — 触发财务同步（Baostock 季频）\n" +
            "- `GET /main/stock/financial/{symbol}` — 标的已披露季频财务（PIT 近8期）\n" +
            "- `POST /main/stock/cash/adjust?amount=&note=` — 手工入金（amount>0）/出金（amount<0）\n" +
            "- `GET /main/stock/cash/log?limit=` — 现金流水（近 N 条）\n";
    }

    /** 交易记录 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/tradeLog")
    public AjaxResult tradeLog() {
        return success(stockAdvisorService.getTradeLog());
    }

    /** 组合表现（权益曲线） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/performance")
    public AjaxResult performance() {
        return success(stockAdvisorService.getPortfolioPerformance());
    }

    /** K 线数据 + 信号标注 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/kline/{symbol}")
    public AjaxResult kline(@PathVariable String symbol,
                            @RequestParam(value = "year", required = false) String year) {
        return success(stockAdvisorService.getKlineData(symbol, year));
    }

    /** 回测任务状态（读任务日志最近一次；回测在【系统监控-定时任务】里手动执行） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/backtest/status")
    public AjaxResult backtestStatus() {
        return success(stockAdvisorService.getBacktestJobStatus());
    }

    /** 因子任务状态（只读研究数据） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/factor/status")
    public AjaxResult factorStatus() {
        return success(stockAdvisorService.getFactorStatus());
    }

    /** 因子结论列表（runId 为空取最新） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/factor/evals")
    public AjaxResult factorEvals(@RequestParam(required = false) String runId) {
        return success(stockAdvisorService.getFactorEvals(runId));
    }

    /** 因子报告载荷（kind 默认 summary） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/factor/report")
    public AjaxResult factorReport(@RequestParam(required = false) String runId,
                                   @RequestParam(required = false) String kind) {
        return success(stockAdvisorService.getFactorReport(runId, kind));
    }

    /** 因子运行历史 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/factor/runs")
    public AjaxResult factorRuns(@RequestParam(defaultValue = "20") Integer limit) {
        return success(stockAdvisorService.getFactorRuns(limit));
    }

    /** 组合级回测结果（单一共享现金账户：初始10w → 期末总资产，含每日净值与期末持仓） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/backtestPortfolio")
    public AjaxResult backtestPortfolio(@RequestParam(required = false) Long runId) {
        return success(stockAdvisorService.getPortfolioBacktest(runId));
    }

    /** 组合级回测历史批次（每用户保留最近 10 次，供前台切换对比） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/backtestPortfolio/runs")
    public AjaxResult backtestPortfolioRuns() {
        return success(stockAdvisorService.listPortfolioRuns());
    }

    /** 指定批次的组合逐笔成交；不传 runId 取最新一次 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/backtestPortfolio/trades")
    public AjaxResult backtestPortfolioTrades(@RequestParam(required = false) Long runId) {
        return success(stockAdvisorService.getPortfolioTrades(runId));
    }

    /** 系统运行状态（数据同步、Tushare连通、邮件配置等） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/systemStatus")
    public AjaxResult systemStatus() {
        return success(stockAdvisorService.getSystemStatus());
    }

    /** 新增持仓 */
    @PreAuthorize("@ss.hasPermi('main:stock:edit')")
    @Log(title = "持仓管理", businessType = BusinessType.INSERT)
    @PostMapping("/holding")
    public AjaxResult addHolding(@RequestParam String symbol,
                                  @RequestParam String name,
                                  @RequestParam Integer shares,
                                  @RequestParam Double costPrice) {
        stockAdvisorService.addHolding(symbol, name, shares, costPrice);
        return success();
    }

    /** 更新持仓 */
    @PreAuthorize("@ss.hasPermi('main:stock:edit')")
    @Log(title = "持仓管理", businessType = BusinessType.UPDATE)
    @PutMapping("/holding/{symbol}")
    public AjaxResult updateHolding(@PathVariable String symbol,
                                     @RequestParam(required = false) Integer shares,
                                     @RequestParam(required = false) Double costPrice) {
        stockAdvisorService.updateHolding(symbol, shares, costPrice);
        return success();
    }

    /** 删除持仓 */
    @PreAuthorize("@ss.hasPermi('main:stock:edit')")
    @Log(title = "持仓管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/holding/{symbol}")
    public AjaxResult deleteHolding(@PathVariable String symbol) {
        stockAdvisorService.deleteHolding(symbol);
        return success();
    }

    /** 股票代码/名称互查（全 A 股 stock_basic_cache，用于新增持仓时自动带出另一项） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/symbols/lookup")
    public AjaxResult lookupSymbols(@RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        if (q == null || q.trim().isEmpty()) return success(java.util.Collections.emptyList());
        return success(stockAdvisorService.lookupSymbols(q.trim(), limit == null ? 10 : limit));
    }

    /** 搜索已下载标的（支持非持仓，基于 stock_daily_quote + stock_basic_cache） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/symbols/search")
    public AjaxResult searchSymbols(@RequestParam(value = "q", required = false) String q,
                                    @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        if (q == null || q.trim().isEmpty()) return success(java.util.Collections.emptyList());
        return success(stockAdvisorService.searchDownloadedSymbols(q.trim(), limit == null ? 20 : limit));
    }

    /** 获取孤儿持仓列表（有持仓但无策略的标的） */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @GetMapping("/orphanHoldings")
    public AjaxResult orphanHoldings() {
        return success(stockAdvisorService.getOrphanHoldings());
    }

    /** 为孤儿持仓分配策略 */
    @PreAuthorize("@ss.hasPermi('main:stock:list')")
    @Log(title = "股票顾问", businessType = BusinessType.OTHER)
    @PostMapping("/assignOrphanStrategies")
    public AjaxResult assignOrphanStrategies() {
        return success(stockAdvisorService.assignOrphanStrategies());
    }
}
