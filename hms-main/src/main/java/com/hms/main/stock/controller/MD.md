## 股票持仓顾问接口

add by CYQ 2026-06-05

### 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /main/stock/overview | 持仓概览（总资产、市值、现金、待处理建议数） |
| GET | /main/stock/holdings | 持仓列表（含分配策略和市场状态） |
| GET | /main/stock/recommendations?pageNum=&pageSize=&orderByColumn=&isAsc= | 操作建议列表（分页，返回 total/rows；`orderByColumn` 支持 createdTime/symbol/name/ownerName/action/priceHint/suggestedShares/suggestedAmount/actualPrice/actualShares/reason/confidence/status，`isAsc` 取 asc/desc，缺省按 createdTime 降序） |
| POST | /main/stock/analyze | 手动触发一次 Python 分析 |
| PUT  | /main/stock/execute/{id}?price=&shares= | 标记建议已执行（可传实际成交单价/股数） |
| GET  | /main/stock/performance | 组合权益曲线（按现金/交易流水重建每日持仓与现金，累计收益率=时间加权 TWR，已剔除入金/出金） |
| GET  | /main/stock/backtestPortfolio | 组合级回测结果（共享现金账户），可选 `?runId=` 指定批次，缺省取最新一次 |
| GET  | /main/stock/backtestPortfolio/runs | 组合级回测历史批次列表（每用户最近 10 次） |
| GET  | /main/stock/backtestPortfolio/trades?runId= | 指定批次的组合逐笔成交，缺省取最新一次 |

### 返回格式

#### overview
```json
{
  "stockCount": 7,
  "totalValue": 45176.40,
  "totalPl": -3615.88,
  "availableCash": 10000,
  "pendingCount": 0
}
```

#### holdings item
```json
{
  "symbol": "600588",
  "name": "用友网络",
  "shares": 1600,
  "costPrice": 13.687,
  "currentPrice": 10.70,
  "marketValue": 17120.00,
  "profitLossPct": -22.34,
  "assignedStrategy": "bollinger_mean_reversion",
  "regime": "oscillating"
}
```

#### recommendations item
```json
{
  "id": 1,
  "symbol": "600588",
  "name": "用友网络",
  "strategyName": "bollinger_mean_reversion",
  "action": "BUY",
  "priceHint": 10.70,
  "suggestedShares": 100,
  "suggestedAmount": 1070.00,
  "actualPrice": 10.85,
  "actualShares": 200,
  "reason": "触及布林下轨，RSI超卖",
  "confidence": "high",
  "status": "pending",
  "createdTime": "2026-06-05T14:30:00"
}
```

> 2026-07-03: 新增 `name` 字段（股票名称）
> 2026-08-04: 新增 `actualPrice` / `actualShares` 字段（实际执行时维护的实际成交单价/股数，仅已执行且有维护值时返回）

#### backtestPortfolio
组合级回测：**单一现金账户**，多标的并发持仓，逐交易日重放实盘决策链（含 PIT 财务闸门）。

```json
{
  "exists": true,
  "runId": 20260906164100,
  "backtestYear": 2026,
  "initialCapital": 100000.00,
  "finalValue": 120151.23,
  "finalCash": 50879.23,
  "finalMarketValue": 69272.00,
  "totalReturnPct": 20.1512,
  "annualReturnPct": 32.5900,
  "maxDrawdownPct": -10.5800,
  "sharpeRatio": 1.3930,
  "calmarRatio": 3.0810,
  "winRate": 46.58,
  "tradeCount": 174, "buyCount": 101, "sellCount": 73,
  "realizedPnl": 21106.25, "buyFee": 505.00, "sellFee": 970.59,
  "symbolsCount": 40, "tradingDays": 164,
  "maxConcurrentPositions": 11,
  "avgCashUtilizationPct": 46.48,
  "cashRejectContended": 39,
  "navCurve": [{ "d": "2026-01-05", "nav": 100000.0, "cash": 100000.0, "mv": 0.0, "pos": 0 },
               { "d": "2026-01-06", "nav": 100061.0, "cash": 85035.0, "mv": 15026.0, "pos": 1 }],
  "positions": [{ "symbol": "600089", "name": "特变电工", "shares": 1200,
                  "avg_cost": 19.0017, "market_value": 22656.00, "profit_pct": -0.64 }],
  "params": { "capital": 100000, "portfolio": {}, "cost_model": {}, "pool": {}, "lookahead_warning": "..." }
}
```

`cashRejectContended` = 有持仓时因现金已被其它标的占用而放弃买入的次数。
`params` 为本次口径快照，用于复现。

⚠️ 标的池为「当前持仓 + 当前成交额 topN」的**今日快照**，回测历史含前视与幸存者偏差，
结果偏乐观 —— 本接口属持仓复盘，不是无偏回测。

> 2026-09-06: 新增组合级回测三接口（`backtestPortfolio` / `runs` / `trades`）
> 2026-09-09: 定时任务回测LIVE 优化：Python 侧 `save-results` 输出 `__RESULT_JSON__` 结构化摘要
> （symbols_ok/avg_annual/worst_drawdown/insufficient/pool_width），Java 侧解析后记紧凑日志，
> 避免 sys_job_log 2000 字符截断；`metrics/portfolio` 补 `evidence_status` 三值
> （PASS/FAIL/INSUFFICIENT）与 `empty_days` 空仓期，评价以避损（最大回撤）为主、胜率仅诊断
> 2026-09-10: 删除单标的 LIVE 口径（`backtestResults` / `backtestTrades` 接口、
> `stock_backtest_result` 写入、`backtest/engine|runner|strategy_bridge|plot|save_backtest_results`），
> 回测只保留组合级 PORTFOLIO 口径
> 2026-09-10: `runBacktestTask` 支持年份参数（`invokeTarget` 示例）：
> `runBacktestTask()`=默认年 / `runBacktestTask('2024')` / `runBacktestTask('2024-2026')` /
> `runBacktestTask('2024,2026')`；期间一次取全、单次连贯撮合、单个 run_id 落库，
> `backtest_year` 存起始年、完整范围进 `params_json`

## 因子研究接口（2026-11-13，只读）

> 数据源 `stock_factor_run` / `stock_factor_eval` / `stock_factor_report`（全局研究数据，无 user_id）。
> 触发方式：Quartz「A股因子评估」任务（系统监控 → 定时任务 → 执行一次）或服务器 CLI，
> **不提供** HTTP 同步触发（因子计算 5~15 分钟，避免占用 Tomcat 线程与数据库连接）。

- `GET /main/stock/factor/status` — 最新 run + running 判定（heartbeat ≤5min）+ 行情最新日 + 结论计数
- `GET /main/stock/factor/evals?runId=` — 因子结论列表（runId 缺省取最新；按 PASS/FAIL/INSUFFICIENT 排序）
- `GET /main/stock/factor/report?runId=&kind=` — 报告载荷 JSON（kind 缺省 `summary`）
- `GET /main/stock/factor/runs?limit=20` — 运行历史（limit 1~50）

返回示例（evals）：

```json
[{ "factor_name": "rev_20", "verdict": "PASS", "ic_mean": 0.0643, "icir": 0.375,
   "coverage": 0.999, "ls_annual": 0.289, "monotonicity": 0.94, "turnover": 0.194,
   "n_days": 940, "fdr_p": 0.0, "data_max_date": "2026-11-10" }]
```

> 权限：全部 `main:stock:list`；页面入口为「天才交易员系统 → 因子研究」（`main/stock/factor`）。
