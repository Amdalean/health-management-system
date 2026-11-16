# 移动端股票模块检查清单（CHECKLIST_MOBILE）

> 任务 t26 / t29 · reconstructor · 2026-08-16
> 本页记录移动端（RuoYi-App / uni-app）的股票「持仓 + 待执行建议 + 确认执行」功能，
> 包含：新增文件、路由、接口、依赖的后端接口、以及「自助开通」方案选择。
> **t29 更新**：持仓/建议两页合并为单页 `pages/stock/index.vue`（顶部 segmented 切换），宫格去重为单一「股票」入口。

---

## 1. 新增文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `RuoYi-App/api/main/stock.js` | API | 股票相关接口封装（复用 `@/utils/request`） |
| `RuoYi-App/pages/stock/index.vue` | 页面 | 股票单页：顶部 segmented「持仓 / 建议」两 tab；持仓点击某股切到建议并按 symbol 预筛选；下拉刷新；确认执行/忽略按钮 |
| `RuoYi-App/pages.json` | 配置 | 注册 `pages/stock` subpackage（仅 `index`） |
| `RuoYi-App/pages/work/index.vue` | 修改 | 工作台宫格加单一「股票」入口（RBAC 过滤） |
| `docs/CHECKLIST_MOBILE.md` | 文档 | 本检查清单 |

> ⚠️ **t29 已删除** `pages/stock/holdings.vue` 与 `pages/stock/pending-recs.vue`（旧两页），全部功能并入 `index.vue`；全库已确认无残留跳转旧页的引用。

> 依赖关系：`api/main/stock.js` → `@/utils/request`（已有，Bearer Token 自动附带）；页面样式沿用现有 subpackage（document 等）规范。

---

## 2. 路由 / 页面

在 `pages.json` 的 `subPackages` 中新增：

```json
{
  "root": "pages/stock",
  "pages": [
    { "path": "index", "style": { "navigationBarTitleText": "股票", "enablePullDownRefresh": true } }
  ]
}
```

页面导航：
- 工作台宫格 → `uni.navigateTo({ url: '/pages/stock/index' })`
- 外部可直接进建议 tab：`url: '/pages/stock/index?tab=recs&symbol=600588'`（可选）
- **持仓点某只股票** → 切到建议 tab 并按 `symbol` 预筛选（同页内切换，不再新开页面）

---

## 3. API 封装（`api/main/stock.js`）

| 函数 | 方法/路径 | 说明 |
|------|-----------|------|
| `getStockHoldings()` | `GET /main/stock/holdings` | 查持仓（返回 `StockHoldingVO[]`） |
| `getPendingRecommendations()` | `GET /main/stock/recommendations/pending` | 查 pending 建议（返回 `StockRecommendationVO[]`，status=='pending'） |
| `executeRecommendation(id, data)` | `PUT /main/stock/execute/{id}` | 确认执行（data 传 `{}`，price/shares 缺省回退建议值） |
| `expireRecommendation(id)` | `PUT /main/stock/expire/{id}` | 忽略建议（不执行交易） |

**数据字段（VO）**：
- `StockHoldingVO`：`symbol, name, shares, costPrice, currentPrice, marketValue, profitLoss, profitLossPct, assignedStrategy, regime`
- `StockRecommendationVO`：`id, symbol, name, strategyName, action, priceHint, suggestedShares, suggestedAmount, price, actualShares, reason, confidence(high/medium/low), status(pending), createdTime`

---

## 4. 依赖的后端接口（已有，无需新增）

均来自 `hms-main/.../stock/controller/StockAdvisorController.java`（`@RequestMapping("/main/stock")`）：

| 后端接口 | 权限标识 | 已存在 |
|----------|----------|:------:|
| `GET /main/stock/holdings` | `main:stock:list` | ✅ |
| `GET /main/stock/recommendations/pending` | `main:stock:list` | ✅ |
| `PUT /main/stock/execute/{id}` | `main:stock:list` | ✅ |
| `PUT /main/stock/expire/{id}` | `main:stock:list` | ✅ |

> 移动端复用以上现有接口，后端 URL 不变。**多用户改造（t22/t23/t24/t25）落地后**，这些接口将由后端按 `SecurityUtils.getUserId()` 自动按用户过滤，移动端无需改 URL / 页面逻辑。

---

## 5. 工作台宫格 RBAC 入口

与现有宫格一致，在 `pages/work/index.vue` 的 `allMenus` 增加（t29 去重为单一入口）：

```js
{ id: 'stock', icon: 'briefcase', text: '股票', perm: 'main:stock:list', url: '/pages/stock/index' }
```

`visibleMenus` computed 用 `$auth.hasPermi(item.perm)` 过滤（admin 恒可见，权限未就绪时隐藏）。权限数据来自登录 `/getInfo` 返回的 `permissions[]`。

---

## 6. 自助开通流程（选用方案 + a/b 说明）

「自助开通」指新用户如何获得移动端股票功能（`main:stock:*` 权限）访问权。结合 `docs/MULTIUSER_PLAN.md` 开放问题 §6，提供两个方案，**本任务选用「方案 a（admin 手工授权）」作为当前落地方式**：

### 方案 a（当前采用）— Admin 手工在 PC 端授权
- **流程**：新用户注册 → Admin 登录 PC 端 → 「系统管理 → 用户管理」确认用户 exists → 「角色管理」给该用户分配含 `main:stock:list` / `main:stock:edit` 的角色（如「股票顾问」）→ 用户重新登录移动端，工作台宫格自动出现「股票持仓/股票建议」。
- **优点**：零后端改动、安全可控（管理员审核授予）、与 RuoYi 既有 RBAC 天然一致。
- **缺点**：需人工介入，非即时开通。
- **落地**：移动端无需任何额外代码；「股票」宫格因 RBAC 过滤对无权限用户自动隐藏。

### 方案 b（备选 / 后续迭代）— 移动端自助开通 UI
- **流程**：移动端注册/「我的」页提供「开通股票账户」按钮 → 调用新后端接口（如 `POST /main/stock/activate`）→ 后端为该用户自动写入 `sys_role`（含 `main:stock:*`）并初始化 `account_cash`（user_id）→ 前端刷新权限 → 宫格出现。
- **需新增后端**：`POST /main/stock/activate`（权限校验 + 幂等开通 + 初始化个性化 `account_cash`），属多用户改造（t24）的一环。
- **优点**：即时自助、体验好。
- **缺点**：无人工审核（需决定是否加风控/限额）、需新增后端接口与权限临时下发的安全考量。

**结论**：本期因后端多用户权限链路（t23/t24/t25）仍在开发中，**采用方案 a**（admin 手工授权）即可让移动端功能按 RBAC 先行上线；**方案 b** 作为「自助即时开通」的后续迭代项，待多用户改造稳定后实现（挂在 `main:stock:activate` 后端接口上）。

---

## 7. 验证建议

- `pages.json` JSON 合法（已用 `json.load` 校验通过）。
- 手动：工作台宫格在「有 `main:stock:list` 权限」用户可见单一「股票」项；无权限则隐藏；点击进入 `pages/stock/index`。
- 持仓 tab：`GET /main/stock/holdings` 返回数组 → 每只股票一张卡片；点击某股切到建议 tab 并按 symbol 预筛选。
- 建议 tab：`GET /main/stock/recommendations/pending` 返回数组 → 「确认执行」调 `PUT .../execute/{id}`（弹窗确认后执行）、「忽略」调 `.../expire/{id}`。
- 下拉刷新：分别刷新当前激活 tab 的数据。

> ⚠️ 本任务内存量：沙箱无 HBuilderX / 移动端运行环境，无法在此端编译运行 uni-app，需在 HBuilderX 或 CI 打包验证。
