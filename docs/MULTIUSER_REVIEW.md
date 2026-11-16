# HMS 股票多用户改造综合复核报告 (MULTIUSER_REVIEW)

- 复核人: reviewer（审核者/质量把关）
- 复核范围: t23（DB 迁移 SQL）/ t24（Java）/ t25（Python）/ t26（移动端）四层多用户改造
- 参考方案: `docs/MULTIUSER_PLAN.md`（t22）；各层清单：`CHECKLIST_MULTIUSER_JAVA.md`(t24)、`veighNa/docs/CHANGE_LOG.md` t25 段、`CHECKLIST_MOBILE.md`(t26)
- 复核方式: 只读代码对读 + 跨层契约比对（不改任何代码）
- **总体结论: ⚠️ 有条件通过（PASS with one critical gap + minor notes）**

---

## 🚨 最高优先级缺陷（务必先修）

### Java 传 `--user-id`，但 `advisor/scheduler.py` / `advisor/scanner.py` 的 `__main__` 未解析 → 用户隔离失效（跨层断裂）

- Java（t24）`runAnalysis()` / `runScanner()` 直接以模块方式调 Python：
  - `StockAdvisorServiceImpl.java:348-352` → `-m advisor.scheduler --user-id <uid>`
  - `...:385-389` → `-m advisor.scanner --user-id <uid>`
- 但 Python 侧这两个模块的 `__main__` 块：
  - `veighNa/advisor/scheduler.py:641-642` → `if __name__=="__main__": run_analysis()`（无 argv）
  - `veighNa/advisor/scanner.py:763-764` → `if __name__=="__main__": run_scan()`（无 argv）
  - 二者**都不从 `sys.argv` 解析 `--user-id`** → `user_id=None`。
- 后果：
  1. Java 手动「分析/扫描」调 Python 时，Python 跑在 **全局(None)** 模式：`save_to_scan_result` 写 `stock_scan_result` 无 user_id（留 0），而 Java `getScanResults()`（`WHERE user_id=<uid>`）读不到 → **当前用户看不到自己的扫描结果**。
  2. `run_analysis` 同理，写 stock_strategy/recommendation 无 user_id → 归属错乱。
- 对比（正确的例子）：`advisor/assign_orphans.py:138-200` 的 `main()` **有** `--user-id` 解析，Java `-m advisor.assign_orphans ... --user-id` 生效 ✅。
- **修复建议**（二选一）：(a) Java 改走 `main.py scheduler/scanner ... --user-id`（main.py:51/57 已透传）；或 (b) 在 scheduler/scanner 的 `__main__` 加 `--user-id` 解析后传 `run_*`。

> ❗ 这是「分析/扫描」两条主链路的多用户隔离失效，属功能性缺陷，建议在修复前**不要上线**多用户。

---

## 1. 跨层一致性 — ✅（除上述缺陷外）基本对齐

| 一致性项 | 状态 | 依据 |
|---|---|---|
| DB 列名 `user_id` | ✅ 三层一致 | t23 DDL `ADD COLUMN user_id BIGINT NOT NULL DEFAULT 0`；Java `WHERE user_id=?`/`userScope()`；Python `user_id = %s`/INSERT user_id 列 |
| 唯一键 `(user_id,symbol)` / `(user_id,symbol,strategy_name)` | ✅ | t23:148,159；Python `ON DUPLICATE KEY UPDATE`（scheduler.py 用新复合键去重） |
| Java 写列名 vs DDL | ✅ | `INSERT INTO stock_holding (user_id, symbol, ...)` (Java:551,1150)、`account_cash (user_id,...)`(:581)、`stock_trade_log (user_id,...)`(:587)、`stock_scan_result (user_id, scan_time, ...)`(:437) 均与 t23 对齐 |
| `--user-id` 参数名 | ⚠️ | main.py 与 assign_orphans 用 `--user-id`（与 Java 一致），**但 scheduler/scanner `__main__` 未解析**（见顶部 🔴） |
| 移动端接口路径 | ✅ | `/main/stock/holdings`、`/recommendations/pending`、`PUT /execute/{id}`、`PUT /expire/{id}`，与 Controller 一致（Controller:36/84/94/104） |

**t23 落盘位置**：SQL 写在 `veighNa/sql/init_multiuser.sql`，但任务约定为「父仓库 /mnt/e/space/IDE/hms/sql/」。文件完整且内容正确，仅**路径与约定不符**，部署/运维执行前需确认统一路径（否则可能找不到脚本）。⚠️

---

## 2. Java（t24）— ✅ 整体正确

| 复核项 | 状态 | 依据 |
|---|---|---|
| helpers 在位 | ✅ | `currentUserId()`(Java:151-157)、`isAdminUser()`(:162-165)、`userScope()`(:172-177，admin→"1=1"，否则 `user_id=<uid>`) |
| 读方法全部 userScope | ✅ | getOverview(:187,199,213,223,233)、getHoldings(:260,293,301,307)、getScanResults(:626)、getTradeLog、getPortfolioPerformance、getKlineData(:778,仅 name)、getBacktestResults(:882)、getSystemStatus(:964)、getOrphanHoldings(:1206) |
| 未登录处理 | ✅ | `currentUserId()` 对 `uid==null` 抛 `ServiceException("未登录...")`(:153-154)；userScope 对未登录也会经 currentUserId 抛（非静默返回全部） |
| admin 语义 | ✅ | 读=userScope→"1=1"（看全部）；写按 owner/current user（markExecuted 用 ownerId） |
| markExecuted 越权 | ✅ | 读 rec 带 `user_id`(:477) → `!isAdminUser() && recUserId!=uid` → 抛「无权执行他人的建议」(:489-491)；写用 `ownerId`(:512,581,587) |
| markExpired 越权 | ✅ | `WHERE id=? AND status='pending' AND user_id=?`，rows==0 抛「无权限」(:609) |
| runScanner/runAnalysis 传 --user-id | ⚠️ | 传了(:352,389)，但**Python 未消费**（见顶部 🔴） |
| 邮件按用户 | ✅ | sendSignalNotification 查 `r.user_id` JOIN sys_user.email、按 user_id 分组每组独立 sendTo（:1043） |
| 全局表不动 | ✅ | runSyncMarketData(L648)、stock_daily_quote 行情查询(:719,784,944,1010)、回测全局计数不动 |

> 注：Java 无法在沙箱 javac（无 Linux JDK），结构已配平+静态核对；最终编译需 Windows 侧 `mvn -pl hms-main -am compile`（清单已注明）。

---

## 3. Python（t25）— ⚠️ 后向兼容正确，但模块入口缺 `--user-id`

| 复核项 | 状态 | 依据 |
|---|---|---|
| `user_id=None` 后向兼容 | ✅ | 全模块 `if user_id is not None:` 才生成 user_id SQL（scheduler/scanner/recommendation/cleanup/mysql_scanner/assign_orphans/save_backtest_results）；不迁移旧库也能跑 |
| scheduler 各 SQL 过滤 | ✅ | run_analysis `_uf()`(:358)，strategy 写(:446)、rec 写(:571)、风险冷却 `user_id=...`(:192/203/205/497) |
| scanner 过滤+写入 | ✅ | 持仓读(:181)、`save_to_recommendation/scant_result(results,user_id)`(:556/640) 含 `user_id is not None` 分支 |
| recommendation/cleanup | ✅ | `expire_opposite_pending(...,user_id)`(:593)、cleanup_recommendations(user_id)(:626) |
| assign_orphans | ✅ | `main()` 解析 `--user-id`(:143-148,153) → detect/assign(按 user) |
| save_backtest_results | ✅ | 解析 `--user-id`，写 backtest_result 带 user_id |
| sync_market_data 未改 | ✅ | 行情全局（git diff 未含） |
| **main.py scheduler/scanner 透传** | ⚠️ | main.py:51,57 透传 ✅；但 Java **不走 main.py**，直接 `-m advisor.scheduler/scanner` → 模块自身 `__main__` 未解析 → 缺陷见顶部 🔴 |

---

## 4. 移动端（t26）— ✅ 接口/页面对齐，一处契约注意

| 复核项 | 状态 | 依据 |
|---|---|---|
| 新增页面 | ✅ | `RuoYi-App/pages/stock/holdings.vue` + `pending-recs.vue` 存在 |
| api 封装 | ✅ | `api/main/stock.js`：getStockHoldings/getPendingRecommendations/executeRecommendation/expireRecommendation，路径与 Controller 一致 |
| pages.json subpackage | ✅ | `pages/stock` 注册 holdings/pending-recs（BOM 但合法 JSON，uni-app 惯例） |
| RBAC 宫格入口 | ✅ | pages/work/index.vue:60-61 加「股票持仓/股票建议」，`perm:'main:stock:list'` 过滤 |
| 执行弹窗→execute | ✅ | pending-recs.vue:92-99 `executeRec→executeRecommendation(item.id,{})`；expire(:108-115) |
| 数据绑定 | ✅ | holdings.vue 用 `marketValue/profitLoss` 等 VO 字段聚合 + 下拉刷新 |
| **execute 参数契约** | ⚠️ | 移动端 `executeRecommendation(id,{})` 走 **PUT body** `{}`；后端 `execute`(Controller:84-90) 用 `@RequestParam(required=false) price/shares`（**query 参数**）。当前因传空 `{}` 可跑（两个可空参数缺省回退建议值），但若前端后续要传实际价/股数，放 body 会被后端忽略。建议把接口对齐（前端传 query，或后端改 @RequestBody）。 |

> 端到端 UI 依赖 `main:stock:list` 权限（admin 手工授权方案 a）；HBuilderX 端未编译（沙箱无 uni-app 运行时），需 CI/宿主打包验证。

---

## 5. 边界 / 部署前提 — ⚠️ 必须标注

### 🚨 部署前提：**必须先跑 t23 `init_multiuser.sql` 迁移，否则 Java 直接炸**

- Java 所有用户数据读/写 SQL 都**无条件**嵌 `user_id`（`userScope()` 恒返回 `"1=1"` 或 `"user_id = <uid>"`；写恒 `INSERT/WHERE ... user_id=?`）。
- 若 `stock_*` / `account_cash` 表尚无 `user_id` 列（未跑 t23），则 Java 首次执行 `getOverview`/`getHoldings`/`markExecuted` 等会抛 **`Unknown column 'user_id'`**，接口整体不可用。
- Python 侧因 `user_id=None` 模式不生成 user_id SQL，**相对安全**，但 Java 传参断裂（见 🔴）会让 Python 行为与预期不符。
- **结论（写进上线 checklist）**：升级顺序必须为 **1) 备份 → 2) 执行 `sql/init_multiuser.sql`（幂等）→ 3) 重启/验证 Java + Python → 4) 功能回归**。迁移前不要启动新 Java 版本。

### 未登录
- Java：读用户数据经 `userScope()`→`currentUserId()` 对 null 抛错（安全，不会误看全部）；admin 判定中 uid null → isAdminUser=false。合理。
- 移动端：接口要求 Bearer Token（@/utils/request 自动带），未登录 401。正常。

---

## 6. 逐层总览

| 层 | 判定 | 一句话 |
|---|---|---|
| t23 DB SQL | ✅ 通过 | 7 表 user_id + 存量归 101 + 复合唯一键 + 幂等存储过程；仅落盘路径与约定不一致 |
| t24 Java | ✅（静态） | userScope/越权/admin/邮件分组/--user-id 整体正确；无法沙箱编译待 Windows 验证 |
| t25 Python | ⚠️ | `user_id=None` 兼容与各模块 SQL 正确；**scheduler/scanner `__main__` 未解析 `--user-id`**（主缺陷） |
| t26 移动端 | ✅ | 页面/接口/路由齐全；execute 传参契约小注意 |
| 跨层 | ⚠️ | 列名/唯一键/URL 对齐；`--user-id` 断裂；部署需先跑 t23 |

---

## 7. 必改 / 建议清单

**Critical（上线阻塞）**
1. 修复 `advisor/scheduler.py` / `advisor/scanner.py` 的 `__main__` 不解析 `--user-id`（或让 Java 走 `main.py scheduler/scanner`）——否则 Java 手动分析/扫描的用户隔离失效。

**Major（部署前提）**
2. 明确/统一 `init_multiuser.sql` 落盘路径（当前在 `veighNa/sql/`，约定在父仓 `sql/`）；上线首行必须先跑迁移（备份后），否则 Java Unknown column 崩溃。

**Minor（建议）**
3. 统一移动端 `execute` 传参契约（body vs @RequestParam query）。
4. `getOverview`/`getKlineData` 等高频读在 admin 读全部时仍对全部用户数据聚合——若业务要求「顾问只看被授权用户」，admin「看全部」语义已在方案确认，属设计选择。
5. t23 幂等存储过程依赖默认 sql_mode（未启用 ANSI_QUOTES，CALL 用双引号字符串）——脚本头已注明，若生产启用了 ANSI_QUOTES 需替换为单引号+转义。

---

## 8. 复核结论

**跨层设计正确、大部分实现到位；但存在 1 个功能性缺陷（Java `--user-id` 未在 Python 模块入口被消费）与 1 个部署前提（先跑 t23 迁移）必须处理。** 建议修复 🔴 后再合并上线；其余为小注意点。

---
## 部署记录（captain）

- **DB 迁移已应用**（2026-08-16）：生产库 ry 的 7 张用户表已加 user_id 列 + 存量归 CYQ(101) + 唯一键改 (user_id,symbol)/(user_id,symbol,strategy_name)。备份在服务器 /root/backup_hms/multiuser_before_20260816_201629.sql。
- ⚠️ 脚本 init_multiuser.sql 在 MySQL 5.7 客户端 `source`/`<` 执行时因 DELIMITER+UTF-8 注释解析报错(ERROR 1064 line21)，已改用逐条 `ALTER/UPDATE/INDEX` 直接执行完成（等效迁移）。脚本保留作参考，重跑时若 source 失败可拆条执行。
- 旧应用(jar,无新代码)迁移后仍 HTTP 200 健康。
- **待办**：新 jar 需 Windows/CI `mvn -pl hms-main -am compile` 后部署并回归，才能让多用户代码真正生效。

---
## 端到端编译验证（round 2，captain）

- **真实 javac 编译通过（Round 2）**：在服务器上用 JDK 17.0.8 的 javac，把修改后的 `StockAdvisorServiceImpl.java` 对着原 jar 解压出的全量 classpath(150 items) 编译 → `JAVAC_EXIT=0`，成功产出 `StockAdvisorServiceImpl.class`。证明多用户 Java 改动（SecurityUtils/userScope/admin语义/邮件按用户/越权防护/跨层修复）**语法与类型完全正确**。
- 较此前 t8/t24/t28 的沙箱静态核对更进一步——这是真实编译器通过。
- **剩余开放项**：产出完整可部署新 jar 需完整 maven package（服务器无 maven，建议用 Gitee Go pipeline 或 Windows `mvn package` 后部署），并做 CYQ 回归 + 第二用户隔离实测。代码+DB 迁移+编译验证均已闭环。

---
## 生产修复记录（round 3，captain，CW 访问报错）

**用户报告**：CW/123456 访问股票页报错。

**根因诊断**（通过生产 API 实测）：
- 服务器已部署含多用户代码的 jar（hms-main 作为 BOOT-INF/lib/hms-main-3.8.8.jar 嵌套依赖）。
- `GET /main/stock/holdings`、`/main/stock/recommendations` 报 **`Column 'user_id' in where clause is ambiguous`(500)**。
- 原因：t24 的 `userScope()` 返回**无表别名的裸 `user_id`**，但 holdings 是 `stock_holding h JOIN stock_strategy s`、recommendations 是 `stock_recommendation r JOIN stock_holding h`，两张表都有 user_id → 歧义。

**修复**（本地源码 + 生产 hot-patch）：
- 源码增加 `userScope(String alias)` 重载（返回 `alias.user_id = X`），单表查询仍用无参 `userScope()`。
- getHoldings → `userScope("h")`；getRecommendations ×2 → `userScope("r")`；getOrphanHoldings → `userScope("h")`。
- javac 17 编译通过（userScope() + userScope(String) 两方法齐备）。
- 生产：备份 hms-admin.jar → 用修复 class 以 STORED 重建内层 hms-main jar → Python zipfile 全 STORED 重建外层 fat jar → 重启。
- **验证**：CW 重新登录，9 个股票接口全部 200（原本 holdings/recommendations 500）。CW 数据为空符合"存量归CYQ(101)"的多用户隔离设计。
- 回滚：备份 `/root/backup_hms/hms-admin_backup_20260816_221256.jar`。
