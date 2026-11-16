# HMS 股票顾问多用户改造 — 完整调研与方案设计（MULTIUSER_PLAN）

> 作者: strategy-researcher（团队 veighna-iterate，任务 t22）
> 日期: 2026-08-16　·　性质: **只读调研 + 方案设计，未改任何代码**
> 涉及: Hms 父仓库（Java `hms-main`/`hms-common`/PC前端 `RuoYi-Vue3`/移动端 `RuoYi-App`）+ `veighNa` Python 顾问 + `ry` 数据库
>
> 已确认用户事实（captain 提供 + 复核）：
> - **CYQ** = 现有唯一活跃用户：`user_id=101`、`email=user@example.com`、`user_name`（登录名）待确认。
> - **admin** = `user_id=1`，RuoYi 超级管理员。
> - `stock_holding` 现有 6+ 行（`symbol UNIQUE`，无 `user_id`）。

---

## 0. 结论摘要（先给结论）

1. **这是一个端到端的数据所有权改造**：目前所有 `stock_*` 用户数据表都是**全局单用户**（无 `user_id`），
   核心阻碍是 `stock_holding.symbol` 是**全局唯一**（`UNIQUE KEY uk_symbol`），两个用户无法持有同一只股票。
2. Java 侧**所有**涉及用户数据的 SQL 都必须加 `WHERE user_id = SecurityUtils.getUserId()`（RuoYi `SecurityUtils` 已存在且可用，stock 模块当前**完全未用**）。
3. Python 侧（`veighNa`）是**按 Cron/手动全库操作**，改造成需**输入用户上下文** + 每条用户 SQL 加 `user_id`。
4. **市场数据表（`stock_daily_quote`/`stock_basic_cache`）是全局共享的，不按用户隔离**（多用户应共享同一行情缓存）。
5. 邮箱通知当前 `sendSignalNotification` 发给**单一全局地址**（`MailConfig.getMe()` / `sys.index.mailTo`），需改为按 `sys_user.email` 分组分发。
6. 存量数据（现有持仓/策略/建议/现金流）全部归用户 **CYQ(101)**。
7. 设计原则：**向后兼容 + 渐进式**。先加 `user_id` 列 + 存量迁移 + Java/Python WHERE 过滤；`user_id` 为空的行视为 admin 或全局，逐步收紧。

---

## 1. 现状事实清单（全部经源码复核）

### 1.1 Java 框架已确认
- `hms-common/.../utils/SecurityUtils.java` 标准 RuoYi：`getUserId()/getUsername()/getDeptId()/isAdmin(Long)` 均可用。
  - `SecurityUtils.getUserId()` → `getLoginUser().getUserId()`。
  - `SecurityUtils.isAdmin(userId)`：`userId == 1`（admin）。
- **stock 模块（`hms-main/.../stock/`）当前 0 处使用 SecurityUtils/getUserId** → 完全未做用户隔离。

### 1.2 Java 侧 SQL 触点（`StockAdvisorServiceImpl.java`，1211 行）
> 划分类别：🔵=用户数据(需隔离)　🟢=市场/全局(共享)

| 方法 | 行 | 表 | SQL 是否需按 user_id 过滤 |
|---|---|---|---|
| `getOverview()` | 150-153 | `stock_holding` | 🔵 `WHERE user_id=?` |
| `getOverview()` 现金流 | 164-166 | `account_cash` | 🔵 `WHERE user_id=?` |
| `getOverview()` pending 数 | 176-178 | `stock_recommendation` | 🔵 |
| `getOverview()` 最近分析 | 187-189 | `stock_recommendation` | 🔵 |
| `getOverview()` 最近扫描 | 197-199 | `stock_scan_result` | 🔵 |
| `getHoldings()` | 223-232 | `stock_holding`+`stock_strategy` | 🔵 JOIN 两侧都加 user_id |
| `getRecommendations()`(list) | 249-257 | `stock_recommendation`+`stock_holding` | 🔵 |
| `getRecommendations(page)` | 260-278 | `stock_recommendation` | 🔵 |
| `runScanner()` 清扫描表 | 385 | `stock_scan_result` DELETE | 🔵 `WHERE user_id=?` |
| `runScanner()` 写扫描 | 390-391 | `stock_scan_result` INSERT | 🔵 加 user_id 列 |
| `markExecuted()` **含金量最高** | 425-540 | rec/holding/account_cash/trade_log | 🔵 **全链路加 user_id**；需校验 recommendation 归属当前用户 |
| `markExpired()` | 545-547 | `stock_recommendation` | 🔵 `WHERE user_id=? AND id=?` |
| `getScanResults()` | 553-558 | `stock_scan_result` | 🔵 |
| `getTradeLog()` | 612-615 | `stock_trade_log` | 🔵 |
| `getPortfolioPerformance()` | 633-666 | holding/quotes/account_cash | 🔵 holding+cash；🟢 quotes 行情共享 |
| `getKlineData()` | 708-800 | `stock_daily_quote` | 🟢 行情共享；但 stock_holding name 查询(798) 🔵 |
| `getBacktestResults()` | 812-819 | `stock_backtest_result` | ⚠️ 建议按 user 归属（或全局），见开放问题 |
| `getSystemStatus()` | 838-961 | holding/rec/scan/trade_log/行情 | 🔵 用户持仓/建议部分按 id；🟢 行情/系统配置全局 |
| `sendSignalNotification()` | 969-1040 | `stock_recommendation` | 🔵 查信号；**邮件需按用户分组发** |
| `addHolding()` | 1046-... | `stock_holding` INSERT | 🔵 写入时自动填 `SecurityUtils.getUserId()` |
| `updateHolding()`/`deleteHolding()` | ~ | `stock_holding` | 🔵 `WHERE user_id=? AND symbol=?` |

### 1.3 Java 邮件逻辑（关键改造点）
- `sendSignalNotification()` 行 **1025-1033**：`helper.setTo(MailConfig.getMe().split(","))` — **单一全局收件人**。
- `MailConfig.getMe()` 来自 `sys_config.config_key='sys.index.mailTo'`（行 910-911）。
- **改造**：按 `sys_user.email` 分组 —— 每个用户查其 `stock_recommendation` 的 pending 信号 → 按各自 user_id 对应 `sys_user.email` 单独 `setTo` + 单独发。

### 1.4 Python 侧（`veighNa`）触点
> Python 由 Java `ProcessBuilder` 或手动 Cron 调用，**无 web 会话** → 需要把 user_id 作为参数传入，或改为"按用户循环执行"。

| 模块 | 表单 | SQL | user_id 隔离方式 |
|---|---|---|---|
| `advisor/scheduler.py:340` | `stock_holding` 读 | `SELECT * FROM stock_holding` | 需 `WHERE user_id=%s`（入参 user_id） |
| `scheduler.py:353` | `account_cash` 读 | `SELECT balance ... LIMIT 1` | 需 `WHERE user_id=%s` |
| `scheduler.py:391,416,420` | holding/strategy 写 | UPDATE/INSERT | 加 user_id 列+过滤 |
| `scheduler.py:507,528` | `stock_recommendation` 写 | dedup/insert | 加 user_id |
| `advisor/scanner.py` | holding/rec/scan_result | 读持仓排重+写候选/建议 | 需 user_id（扫描似全局，但产出建议按 user_id） |
| `advisor/mysql_scanner.py` | 同上 | MySQL 回退扫描 | 同上 |
| `advisor/recommendation.py` | `stock_recommendation` | 相反信号消解 | 加 user_id |
| `advisor/cleanup_recommendations.py` | `stock_recommendation` | 清理 | 加 user_id（清理只清当前用户） |
| `advisor/assign_orphans.py` | holding/strategy | 孤儿分配 | 加 user_id |
| `save_backtest_results.py` | holding/backtest_result | 全持仓回测 | 需按用户；（backtest_result 可全局或按用户，见开放问题） |
| `sync_market_data.py` | stock_daily_quote/basic | 行情同步 | 🟢 **全局共享，不加 user_id** |

### 1.5 数据表分类（`veighNa/sql/init_stock_tables.sql` + 运行时）
| 表 | 性质 | 现状键 | 是否需 user_id | 改动 |
|---|---|---|---|---|
| `stock_holding` | 🔵 用户持仓 | `UNIQUE uk_symbol(symbol)` | **是** | ALTER 加 user_id；**【主键/唯一键改 (user_id,symbol)】** |
| `stock_strategy` | 🔵 | `UNIQUE uk_symbol_strategy(symbol,strategy_name)` | **是** | 唯一键改 (user_id,symbol,strategy_name) |
| `stock_recommendation` | 🔵 | idx_symbol | **是** | ALTER 加 user_id + 索引 |
| `stock_trade_log` | 🔵 | idx_symbol | **是** | ALTER 加 user_id |
| `account_cash` | 🔵 | 运行时表 | **是** | ALTER 加 user_id |
| `stock_scan_result` | 🔵(候选) | 运行时表 | **是** | ALTER 加 user_id |
| `stock_backtest_result` | ⚠️ | 运行时表 | **待定**（建议按用户，或全局共享） | ALTER 加 user_id |
| `stock_daily_quote` | 🟢 行情 | `UNIQUE uk_symbol_date` | **否** | 不改 |
| `stock_basic_cache` | 🟢 名称缓存 | | **否** | 不改 |

> **stock_strategy 唯一键说明**：若沿用 `(symbol,strategy_name)` 全局唯一，多用户同持一只票会冲突。需改为 `(user_id, symbol, strategy_name)`。
> **stock_holding 唯一键**：必须从 `(symbol)` 改为 `(user_id, symbol)`，否则两个用户无法持同一股票。

### 1.6 前端/移动端现状
- **PC** `RuoYi-Vue3/src/views/main/stock/`：`index.vue`（持仓）/`kline.vue`/`backtest.vue`/`pending-recs.vue`。接口全部走 `GET /main/stock/*`（`@PreAuthorize('main:stock:list')`）。前端 JS 调用 `request({url:'/main/stock/...'})`，**改造后无需前端改 URL**，由后端按 `SecurityUtils.getUserId()` 过滤即可返回各自数据。（可能需在 VO 里附带 user 角标，见开放问题。）
- **移动端** `RuoYi-App/`：`pages.json` 3 tabBar（首页/工作台/我的）+ subPackages（document/reminder/scoreRecord/jotdown/timeDiary）。**当前无任何股票页面**。
  - 新增方案：新增 subpackage `pages/stock/*`（持仓/建议/kline），并在「首页」或「工作台」宫格菜单加入口（参考 `hms-mobile-permission-filter`：工作台菜单按 RBAC `$auth.hasPermi('main:stock:list')` 动态过滤）。

---

## 2. 设计方案

### 2.1 总体原则
- **数据所有权 = user_id**（`sys_user.user_id`）。所有 🔵 表加 `user_id BIGINT NOT NULL DEFAULT 0`（`0`= 无主/全局，作为过渡；`1`=admin）。
- **安全默认**：Java 用 `SecurityUtils.getUserId()`；`isAdmin` 时**可看全部**（或者 admin 也只见自己的，见开放问题）。
- **Python 无会话**：改为"按用户循环执行" —— 顶层入口入参 `--user-id=<id>` 或 Java 循环每个用户分别调 `-m advisor.scheduler --user-id=xxx`。

### 2.2 DB 迁移 SQL（一次性 ALTER + 存量归 CYQ）
```sql
-- 说明：存量全部归 CYQ(user_id=101)。

-- 1. stock_holding
ALTER TABLE stock_holding
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户(sys_user.user_id, 0=无主/待迁移) AFTER id';
UPDATE stock_holding SET user_id = 101 WHERE user_id = 0;
ALTER TABLE stock_holding DROP KEY uk_symbol;
ALTER TABLE stock_holding ADD UNIQUE KEY uk_user_symbol (user_id, symbol);

-- 2. stock_strategy
ALTER TABLE stock_strategy
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE stock_strategy SET user_id = 101 WHERE user_id = 0;
ALTER TABLE stock_strategy DROP KEY uk_symbol_strategy;
ALTER TABLE stock_strategy ADD UNIQUE KEY uk_user_symbol_strategy (user_id, symbol, strategy_name);

-- 3. stock_recommendation
ALTER TABLE stock_recommendation
    ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE stock_recommendation SET user_id = 101 WHERE user_id = 0;
ALTER TABLE stock_recommendation ADD INDEX idx_user (user_id);

-- 4. stock_trade_log
ALTER TABLE stock_trade_log ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE stock_trade_log SET user_id = 101 WHERE user_id = 0;
ALTER TABLE stock_trade_log ADD INDEX idx_user (user_id);

-- 5. account_cash（若已建）
ALTER TABLE account_cash ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE account_cash SET user_id = 101 WHERE user_id = 0;

-- 6. stock_scan_result（若已建）
ALTER TABLE stock_scan_result ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE stock_scan_result SET user_id = 101 WHERE user_id = 0;

-- 7. stock_backtest_result（若决定按用户）
ALTER TABLE stock_backtest_result ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0 COMMENT '归属用户' AFTER id;
UPDATE stock_backtest_result SET user_id = 101 WHERE user_id = 0;

-- 8. （可选）数据字典/权限
-- 行情表 stock_daily_quote / stock_basic_cache：不动（全局共享）。
```
> ⚠️ `UPDATE ... WHERE user_id = 0` 假设存量无真正的"无主"数据；若 `0` 与业务混淆，可先 `SELECT COUNT(*)` 排查。`account_cash`/`stock_scan_result`/`stock_backtest_result` 的**现有 DDL 未在本仓库的 init SQL 中**（运行时 `ensure_table` 或手工建），需先确认表是否存在再 ALTER（列出为开放问题）。

### 2.3 sys_user → 新增用户接入（供未来第二个用户）
- 新用户在 `sys_user` 建号（RuoYi 管理端），分配 `sys_role` 含 `main:stock:list` / `main:stock:edit` 权限（角色「股票顾问」）。
- 其 `stock_holding.user_id = 该用户 user_id`，各表以其 user_id 隔离。
- email 通知自动读 `sys_user.email` 发给该用户。

### 2.4 Java 改造清单（按方法）
- 在每个 🔵 方法顶部取 `Long uid = SecurityUtils.getUserId()`；若 `uid == null` → 抛 `ServiceException("未登录")`。
- **读方法**：所有 `SELECT ... WHERE user_id = ?`（或 `WHERE user_id IN (...)`，若 admin）。
- **写方法**（`markExecuted`/`addHolding`/`updateHolding`/`deleteHolding`/`markExpired`）：
  - INSERT 显式写 `user_id`。
  - UPDATE/DELETE `WHERE user_id = ? AND (id/symbol) = ?`，**并校验行数**——若 `rowcount==0` 表示非本人数据，拒绝（防越权）。
- `markExecuted` 是**高风险**：改仓/现金/流水/建议状态全链路，需一次性把所有涉及表补 user_id，且**建议归属校验**（recommendation 的 user_id 必须==当前用户才能执行）。
- **admin 语义**（开放问题）：`SecurityUtils.isAdmin(uid)` 是否看全部用户数据？建议：
  - 读：admin 可 `SELECT ... WHERE 1=1`（全部）或 `WHERE user_id IN (SELECT ...)`。
  - 写：即便 admin 也按具体 user_id（避免改错人）。

### 2.5 Python 改造清单
- `advisor/scheduler.run_analysis(cfg, user_id=None)`：
  - `SELECT * FROM stock_holding WHERE user_id=%s`；
  - 所有写入 stock_strategy/stock_recommendation 加 user_id（赋值入参）；
  - `account_cash` 读 `WHERE user_id=%s`（无则默认 20000，保留现值）。
- `advisor/scanner.run_scan(cfg, top_n, user_id=None)`：持仓排重 `WHERE user_id=%s`；写 scan_result/recommendation 加 user_id。
- 入口：`main.py scanner/scheduler --user-id=xxx`；**Java `runAnalysis`/`runScanner` 改为遍历所有启用用户或只当前用户调用**（开放问题：是每个用户跑自己的分析，还是只对发起用户？建议——`/analyze`由用户手动触发时只跑该用户；Cron 全局扫描每个用户各跑一次）。
- `sync_market_data.py`：**不改 user 隔离**（行情全局）。
- `save_backtest_results.py`：回测按用户 holdings；写入 user_id。

### 2.6 邮件按用户分类发送
- `sendSignalNotification(source)`：
  1. 查 `SELECT DISTINCT user_id FROM stock_recommendation WHERE status='pending' AND created_time>=...`。
  2. 对每个 user_id：查 `sys_user.email WHERE user_id=?`（且 `email` 非空）。
  3. 该用户的 pending 信号构建一个 HTML → `setTo(该用户 email)` → `mailSender.send`。
  4. 有多个用户就发多封（各自只含自己的信号）。
- 回退：`email` 为空 → 跳过或发到 `MailConfig.getMe()`（全局）。
- `ReminderTask.java:126` 也发邮件给 `MailConfig.getMe()`——若也涉及股票相关需同步改，否则不动。

### 2.7 PC / 移动端
- **PC**：接口 URL 不变，UI 无需大改；如需显示当前用户标识，`index.vue` 顶部加 `userName`（从 `/getInfo` 拿用户名）。
- **移动端新增**：
  - `RuoYi-App/pages/stock/holdings.vue`（持仓）、`pages/stock/pending-recs.vue`（建议）、`pages/stock/kline.vue`。
  - `pages.json` 注册 subpackage `pages/stock`；首页/工作台宫格加股票入口（RBAC `main:stock:list` 过滤）。
  - 复用 `uni.request` + 现有 `main:stock/*` 接口（后端已按 user_id 过滤）。

---

## 3. 实施顺序（分阶段，降低风险）
1. **Phase 0**：备份/快照 `stock_*` + `account_cash` + `sys_user` 数据。
2. **Phase 1（DB）**：执行 §2.2 迁移 SQL，存量归 CYQ(101)；确认 `account_cash/stock_scan_result/stock_backtest_result` 是否存在并 ALTER。
3. **Phase 2（Java 只读）**：`getOverview/getHoldings/getRecommendations/getTradeLog/getPortfolioPerformance/getSystemStatus/getScanResults/getKlineData(getName)` 加 `WHERE user_id=SecurityUtils.getUserId()`（admin 例外看策略）。回归「CYQ 登录看数据不变」。
4. **Phase 3（Java 写 + 越权防护）**：`markExecuted/addHolding/updateHolding/deleteHolding/markExpired/runScanner`。
5. **Phase 4（邮件按用户）**：`sendSignalNotification` 按 `sys_user.email` 分组。
6. **Phase 5（Python）**：scheduler/scanner/mysql_scanner/recommendation/cleanup_recommendations/assign_orphans/save_backtest_results 加 user_id；`main.py` + Java ProcessBuilder 传 user_id。
7. **Phase 6（移动端）**：新增 `pages/stock/*` + 导航。
8. **Phase 7（测试）**：建第二个测试用户，验证互相看不到对方持仓/建议/交易，CYQ 数据完整；邮件各自收到自己的。

---

## 4. 需要 captain/user 决策的开放问题（末尾必答）

1. **admin(1) 的语义**：`SecurityUtils.isAdmin` 时，读操作是「看全部用户数据」还是「也只见自己」？建议：读=全部、写=本人（越权防护优先）。
2. **`stock_backtest_result`**：按用户隔离，还是全局共享（任何用户可见全部回测）？建议**按用户**（含 user_id），理由：结果含策略/收益，属个人关注。
3. **`stock_scan_result`**：全市场扫描候选，是「每个用户各扫各的」还是「全局一份、各用户各自可见自己的执行？」建议：扫描结果（候选池）可全局但带 user_id 归属，`getScanResults` 只返回当前用户自己的。
4. **Cron 触发策略**：Java `/analyze`、`/scanner` 手动触发时：只处理当前用户，还是触发"所有启用用户全量分析"？建议：手动=`当前用户`；新增独立 Quartz Cron(遍历所有用户) 另讨论。
5. **存量 `user_id=0` 兜底**：迁移后是否允许 `user_id=0`（无主）新数据产生？建议：Java/Python 写入口**强制填非 0**，`0` 仅存量过渡。
6. ****新用户开通流程**：是否要做「自助开户」UI（移动端注册后自动开股票账户），还是由 admin 在 PC 端手工给 `sys_role`？建议：先 admin 手工。
7. **`account_cash`**：目前现金流是单一记录（`ORDER BY id DESC LIMIT 1`）。多用户后是否要为每个用户建独立现金账户？建议：加 user_id 后每个用户各自一条最新 balance。
8. **每日分析/扫描是否需要按用户排队限流**：Tushare 全市场扫描耗时长，多用户并发会打爆 API 限额 → 是否串行化（建议：Cron 串行逐用户 + 全局扫描合并为一份结果按用户归属）。
9. **权限标识**：是否新增 `main:stock:*` 之外的角色维度（如「只读」「操作」），还是沿用现有 `main:stock:list` / `main:stock:edit`？建议沿用现有。
10. **邮件回退**：用户 `email` 为空时，是跳过还是发到全局 `MailConfig.getMe()`？建议发到全局兜底。

---

## 5. 附：改动涉及文件索引（供落地引用）

**DB**：`veighNa/sql/init_stock_tables.sql`（+ 新增 `sql/init_multiuser.sql` 迁移脚本）
**Java**：
- `hms-main/.../stock/controller/StockAdvisorController.java`（214 行，端点定义，权限已有）
- `hms-main/.../stock/service/impl/StockAdvisorServiceImpl.java`（1211 行，核心改造）
- `hms-main/.../stock/service/IStockAdvisorService.java`
- `hms-common/.../config/MailConfig.java` + `security/SecurityUtils.java`（已有，无需改）
- `hms-common/.../core/domain/entity/SysUser.java`（已有 userId/email）
**Python**：`veighNa/advisor/{scheduler,scanner,mysql_scanner,recommendation,cleanup_recommendations,assign_orphans}.py`、`veighNa/save_backtest_results.py`、`veighNa/main.py`
**PC**：`RuoYi-Vue3/src/views/main/stock/*.vue`（URL 不变，可微调）
**移动端**：`RuoYi-App/pages.json` + 新增 `pages/stock/*` + 工作台/首页宫格（RBAC 过滤）
