# Java 多用户改造改动清单（CHECKLIST_MULTIUSER_JAVA）

> 作者: integrator（团队 veighna-iterate，任务 t24）
> 日期: 2026-08-16
> 文件: `hms-main/src/main/java/com/hms/main/stock/service/impl/StockAdvisorServiceImpl.java`（现 ~1311 行）
> 说明: 本文件为唯一大文件，t24 独占修改（无并发写）。本清单供 Windows 侧最终编译/复核使用。
> 配套: `sql/init_multiuser.sql`（t23，表加 user_id + 存量归 101 + 唯一键改 (user_id,symbol)）、
>       `docs/MULTIUSER_PLAN.md`（t22 方案）
>
> **已确认决策**：admin 读全部写本人；扫描全局一份按 user_id 归属、`getScanResults` 只返回当前用户；
> 手动分析/扫描接口保留只处理当前用户；邮件按 `sys_user.email` 分组发、空则全局兜底。

---

## 0. 编译验证环境（本沙箱）
- WSL/Linux、无系统 java/javac（JAVA_HOME 空、无 /usr/lib/jvm）、apt 无 root——**无法在沙箱跑 mvn/javac**（t8 已确认）。
- 已做：**结构配平（大括号/圆括号 全平衡）、grep 逐处核对每条用户 SQL 含 user_id、逐方法语法审视**。
- **待 Windows 宿主/CI 执行**：`mvn -pl hms-main -am -Dmaven.test.skip=true compile` 做最终编译确认。

---

## 1. 新增 import（2 行）
- 第 16 行 `import com.hms.common.utils.SecurityUtils;`
- 第 17 行 `import com.hms.common.exception.ServiceException;`

## 2. 新增辅助方法（149~179 行，位于 resolvePythonExe 后）
| 方法 | 行 | 说明 |
|---|---|---|
| `currentUserId()` | 151~160 | 取 `SecurityUtils.getUserId()`；null → `throw ServiceException("未登录...")`。写/归属校验用。 |
| `isAdminUser()` | 162~168 | `uid!=null && SecurityUtils.isAdmin(uid)`。读是否看全部。 |
| `userScope()` | 172~179 | 返回 `"1=1"`（admin）或 `"user_id = <uid>"`（普通）。用户数据 SELECT 过滤器。 |

> 安全性说明：`userScope()` 内联的是数字型 userId，无注入风险；仅用于 **读（SELECT）**，写仍用直接 `user_id=?` 参数。

---

## 3. 逐方法改动（方法 → 当前行号 → 改动）

### 读方法（SELECT 加 `WHERE/AND userScope()`）
| 方法 | 行 | 改动 |
|---|---|---|
| `getOverview()` | 180 | holdings 汇总、account_cash、pending 数、最近分析、最近扫描 5 处全部加 `userScope()` |
| `getHoldings()` | 258 | `WHERE scope`；`stock_strategy` JOIN 加 `s.user_id = h.user_id` |
| `getRecommendations()` | 287 | `WHERE userScope()`；holding JOIN 加 `r.user_id=h.user_id`；limit 50 |
| `getRecommendations(page)` | 301 | total `COUNT(*) WHERE userScope()`；rows `WHERE userScope()`+JOIN user_id |
| `getScanResults()` | 621 | `FROM stock_scan_result WHERE userScope() ORDER BY score DESC` |
| `getTradeLog()` | 681 | `FROM stock_trade_log WHERE userScope() ORDER BY trade_time DESC LIMIT 100` |
| `getPortfolioPerformance()` | 703 | holdings `WHERE shares>0 AND userScope()`；cash `WHERE userScope() ORDER BY id DESC LIMIT 1`（行情 quote 全局不动） |
| `getKlineData()` | 778 | `stock_holding` 名称查询加 `userScope() AND symbol=?`（`stock_daily_quote` 行情全局不动） |
| `getBacktestResults()` | 882 | `WHERE userScope()` + 子查询 `MAX(id)` 也 `WHERE userScope() GROUP BY symbol,strategy_type` |
| `getSystemStatus()` | 911 | 持仓汇总/待处理/最近分析/最近扫描/最近交易 5 处加 `userScope()`；行情/系统配置(3/6/7/8/9) 保持全局 |
| `getOrphanHoldings()` | 1206 | `WHERE shares>0 AND userScope()`；NOT EXISTS 的 strategy 子查询加 `s.user_id = h.user_id` |

### 写方法 / 越权防护
| 方法 | 行 | 改动 |
|---|---|---|
| `markExecuted()` | 470 | **全链路 user_id + 归属校验**：读建议带 `user_id` → 校验（普通用户须 `recUserId==current`，否则抛错；admin 可代执行，写入用归属 `ownerId`）；cash/holding/trade_log/更新建议全部 `WHERE/INSERT user_id=ownerId`。新开仓 INSERT 带 user_id。 |
| `markExpired()` | 609 | `WHERE id=? AND status='pending' AND user_id=?`（当前用户）；`rows==0` → 抛 `ServiceException("无权限")` |
| `runScanner()` | 376 | `Long uid=currentUserId()`；ProcessBuilder 加 `--user-id uid`；`DELETE stock_scan_result WHERE user_id=?`；`INSERT ... (user_id,...)` 带 uid |
| `runAnalysis()` | 339 | `Long uid=currentUserId()`；ProcessBuilder 加 `--user-id uid` |
| `assignOrphanStrategies()` | 1279 | `Long uid=currentUserId()`；ProcessBuilder 加 `--user-id uid` |
| `addHolding()` | 1145 | INSERT 加 `user_id`（=当前用户）；写日志带 uid |
| `updateHolding()` | 1163 | `WHERE user_id=? AND symbol=?`（当前用户） |
| `deleteHolding()` | 1194 | `WHERE user_id=? AND symbol=?`（当前用户） |

### 邮件按用户（发送方）
| 方法 | 行 | 改动 |
|---|---|---|
| `sendSignalNotification()` | 1043 | 查询带 `r.user_id` + `LEFT JOIN sys_user ON r.user_id=u.user_id` 取 `email`；按 `user_id` 分组（`Map<Long,List<>>`）；每组构建该用户 HTML → `setTo(该用户 email)` 单独发；email 空 → `MailConfig.getMe()` 兜底；返回总信号数。 |

### 保留全局（不按 user 隔离，符合方案）
- `runSyncMarketData()`：行情同步（`stock_daily_quote`/`stock_basic_cache`）全局共享 → **不改**。
- `getKlineData()` 的 `stock_daily_quote` 行情查询 → **不改**（仅名称查询按 user）。
- `getSystemStatus()` 第 8 项「回测数据状态」`stock_backtest_result` 全局聚合计数 → 保留全局（信息性）。
- `processPendingStopSignals()`（1130+ 行附近）：全局批量 Cron 维护（过期超期信号 + 统计 SELL/REDUCE）→ **不改**。

---

## 4. 涉及的表 → user_id 用法总览
| 表 | 读 | 写 |
|---|---|---|
| `stock_holding` | `userScope()` | INSERT/UPDATE/DELETE 均 `user_id` |
| `stock_strategy` | JOIN `s.user_id=h.user_id` | —（Python 侧写，t25） |
| `stock_recommendation` | 读 `userScope()`；email 用 `user_id` | markExecuted/markExpired 按 `user_id` |
| `stock_trade_log` | `userScope()` | markExecuted INSERT `user_id` |
| `account_cash` | `userScope()` | markExecuted INSERT `user_id` |
| `stock_scan_result` | `userScope()` | runScanner DELETE/INSERT `user_id` |
| `stock_backtest_result` | `userScope()` | —（Python save 侧，t25） |
| `sys_user` | email JOIN | — |
| `stock_daily_quote`/`stock_basic_cache` | **不动**（行情全局） | — |

---

## 5. 静态核对结果（已完成）
- **结构配平**：大括号 153/153、圆括号 935/935，均平衡（无遗漏闭合）。
- **grep 逐处确认**：read 方法每条用户数据 SELECT 都带 `userScope()`/`user_id`；写方法均 `WHERE/INSERT user_id=?`。
- **未遗漏清单**：仅 4 类全局（行情同步、kline 行情、回测全局计数、批量 Cron 止损处理）保持全局，符合设计。
- **新 import/helper** 均在位。

---

## 6. 待 Windows 侧最终验证
1. `mvn -pl hms-main -am -Dmaven.test.skip=true compile`（确认无编译错误；本沙箱无 javac）。
2. 跑 t23 `sql/init_multiuser.sql`（备份后）→ 验证 `stock_*` 表均含 `user_id`、存量归 101、唯一键 `(user_id,symbol)`。
3. 功能回归（CYQ=101 登录看数据应与改造前一致；第二个测试用户看不到对方数据）。
4. 越权回归：用户 B 无法 `execute/expire` 用户 A 的建议、无法读写 A 的持仓/交易/现金。
5. 邮件：两个有 pending 信号的用户各自只收到自己的；email 空者走全局兜底。
6. 并发确认：t25（Python user_id）与 t26（移动端）是否与本 Java 改动的接口契约一致（`/main/stock/holdings` 等 URL 不变，后端按 user 过滤）。

---

## 7. 未改文件
- `StockAdvisorController.java`：**无需改**（URL/权限注解已具备，后端在 Service 层按 user 过滤）。
- `IStockAdvisorService.java`、VO 类：未改。
- `MailConfig`/`SecurityUtils`：已存在，未改。
