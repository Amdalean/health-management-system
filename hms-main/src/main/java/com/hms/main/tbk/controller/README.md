# 淘宝客转链接口（TbkController）

用户自助 H5「粘贴淘宝链接 → 获取隐藏券 + 淘口令」的匿名接口。

- H5 页面：`GET /tbk/index.html`（静态资源，位于 `hms-main/src/main/resources/static/tbk/`）
- 接口前缀：`/tbk/api`
- 鉴权：Controller 标注 `@Anonymous`，无需登录

## 1. 转链

`POST /tbk/api/convert`

请求体：

```json
{
  "content": "https://item.taobao.com/item.htm?id=123456789"
}
```

`content` 支持以下任意格式，服务端自动识别（见 `TbkLinkParser`，拼多多链接见 `PddLinkParser`）：

| 类型 | 示例 |
|---|---|
| 淘宝/天猫商品链接 | `https://item.taobao.com/item.htm?id=123456789` |
| 淘宝短链 | `https://m.tb.cn/h.xxxxxx` |
| 新版分享短链 | `https://e.tb.cn/h.xxxxxx?tk=xxxxxx` |
| 二合一链接 | `https://uland.taobao.com/coupon/edetail?e=xxx` |
| 推广链接 | `https://s.click.taobao.com/xxxx` |
| 淘口令 | `￥AbCdEf12￥` |
| 纯商品ID | `123456789` |
| 拼多多商品链接 | `https://mobile.yangkeduo.com/goods.html?goods_id=123456789` |
| 拼多多短链 | `https://p.pinduoduo.com/xxxxxx` |

响应：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "itemId": "123456789",
    "title": "商品标题",
    "picUrl": "https://img.alicdn.com/xxx.jpg",
    "originalPrice": 99.00,
    "couponAmount": 20.00,
    "finalPrice": 79.00,
    "commissionRate": 12.5,
    "commission": 9.88,
    "cashbackRate": 90.00,
    "cashbackAmount": 8.89,
    "shortUrl": "https://s.click.taobao.com/xxxx",
    "tpwd": "￥AbCdEf12￥",
    "tpwdModel": "08￥ CZ001 AbCdEf12￥ https://m.tb.cn/h.xxxxxx  【商品标题】",
    "itemUrl": "https://item.taobao.com/item.htm?id=123456789",
    "couponInfo": "满99元减20元",
    "demo": false,
    "notice": null,
    "provider": "ztk",
    "copyText": "【商品标题】\n原价 ¥99 → 券后 ¥79（省 20 元）\n复制这条信息，打开手机淘宝：￥AbCdEf12￥\n链接：https://s.click.taobao.com/xxxx"
  }
}
```

失败时 `code` 为 500，`msg` 为可直接展示给用户的中文提示。

- `tpwd` 是纯淘口令（`￥xxx￥`），复制后打开淘宝会自动识别并跳到该商品；
- `tpwdModel` 是淘宝返回的完整口令文案（口令 + `m.tb.cn` 短链 + 标题），适合直接发给好友，
  微信里点短链也能唤起淘宝；前端复制优先用它（add by CYQ 2026-11-10）。
- `cashbackRate` / `cashbackAmount` 是后端按 `tbk.cashback-rate` 折算好的返现比例与预估返利，
  前端只展示、不再自己乘比例，保证与订单表 `cashback_rate` / `cashback_amount` 同口径
  （add by CYQ 2026-11-14 平台服务费，返现比例 90%）。

## 2. 健康检查

`GET /tbk/api/health` → `{"code":200,"msg":"操作成功","data":"tbk ok"}`

## 3. 配置说明（application.yml）

| 配置项 | 说明 |
|---|---|
| `tbk.provider` | `mock`（默认，无需密钥，返回模拟数据）/ `ztk`（折淘客真实转链） |
| `tbk.pid` | 推广位 PID，`mm_xxx_xxx_xxx` |
| `tbk.relation-id` | 渠道关系ID，用于订单归因，可选 |
| `tbk.external-id` | 淘宝客外部用户标记，可选 |
| `tbk.rate-limit-per-minute` | 单 IP 每分钟限流，默认 20 |
| `tbk.cache-seconds` | 转链结果缓存秒数，默认 1800 |
| `tbk.ztk.appkey` / `sid` / `pid` | 折淘客对接密钥与授权账号 |

### 切换到真实转链

1. 注册聚合平台（折淘客 / 大淘客 / 好单库），完成淘宝联盟账号授权，拿到 `appkey`、`sid`、`pid`。
2. 修改 `application.yml`：

```yaml
tbk:
  provider: ztk
  pid: mm_1234567_1234567_123456789
  ztk:
    appkey: 你的appkey
    sid: 你的授权ID
    pid: mm_1234567_1234567_123456789
```

3. 重启后访问 H5 验证；`demo` 字段变为 `false` 即表示走的是真实通道。

### 新增其他聚合平台（如大淘客）

实现 `TbkAggregatorClient` 接口并加条件注解即可，无需改动 Service / Controller：

```java
@Component
@ConditionalOnProperty(prefix = "tbk", name = "provider", havingValue = "dtk")
public class DtkTbkClient implements TbkAggregatorClient {
    @Override
    public TbkConvertResult convert(String material, String itemId) { ... }

    @Override
    public String providerName() { return "dtk"; }
}
```

## 4. 淘宝联盟官方通道（provider=taobao）

现已接入官方 Open API，`application.yml` 中 `tbk.provider: taobao`。

### 4.1 已实测可用的接口（2026-11-03 复核）

| 接口 | 用途 | 状态 |
|---|---|---|
| `taobao.tbk.dg.material.optional.upgrade` | 物料搜索升级版（按标题找商品 + 返回绑定自己推广位的 `click_url`） | ✅ 权限已开通，**当前官方转链主通道** |
| `taobao.tbk.item.info.get` | 商品标题/主图/价格（必须传新商品ID） | ✅ 权限已开通，数字ID报 -4003 |
| `taobao.tbk.spread.get` | 长链转短链 | ✅ 权限已开通 |
| `taobao.tbk.tpwd.create` | 淘口令 | ✅ 权限已开通 |
| `taobao.tbk.privilege.get` | 单品券高效转链 | ⚠️ 可调用但返回 `code=11` 权限包未开通 |
| `taobao.tbk.coupon.convert` | 单品券高效转链（免 session） | ❌ 未申请（scope 12486） |
| `taobao.tbk.dg.material.optional` | 旧版物料搜索 | ❌ 权限包未开通 |

> 官方已禁止数字商品ID转链，`item.info.get` / `coupon.get` 传数字ID会报
> `-4003 / 1912 请使用升级后的新商品ID`。

### 4.2 完成淘宝账号授权（仅 `privilege.get` 需要）

> 生产环境 nginx 把后端挂在 `/prod-api` 前缀下（见 `RuoYi-App/config.js` 的 baseUrl），
> 所以浏览器访问的地址要带 `/prod-api`。

1. 淘宝开放平台 → 应用 → 安全中心 → **回调地址** 填：
   `https://your-domain.com/prod-api/tbk/auth/callback`
2. 浏览器打开 `https://your-domain.com/prod-api/tbk/auth/start`，用联盟账号登录并确认授权。
3. 回调页显示「授权成功」即完成；token 存在 Redis，24 小时自动续期。
4. 查看状态：`GET /prod-api/tbk/auth/status`

> 按标题搜索转链（`material.optional.upgrade` + `spread.get` + `tpwd.create`）都不需要 session。

### 4.3 转链调用的接口选择逻辑

```
带标题的输入（分享文案里通常有「商品标题」）
        ↓
material.optional.upgrade 按标题精确匹配商品 → publish_info.click_url（已绑定 adzone_id）
        ↓ 没匹配到 / 权限异常
coupon.convert（免 session，若有权限）
        ↓ 无权限时自动降级
privilege.get（需要 OAuth session）
        ↓
spread.get 转短链（失败则用长链）
        ↓
tpwd.create 生成淘口令（失败则只返回链接）
```

> `pid` 是 `mm_用户ID_网站ID_推广位ID` 四段，`resolveAdzoneId()` 取最后一段、
> `resolveSiteId()` 取倒数第二段（之前按三段取会把 site_id 当 adzone_id）。

### 4.4 商品ID解析

| 输入 | 处理方式 |
|---|---|
| 商品链接 / 纯ID | 正则直接提取 |
| `m.tb.cn` 短链 | 服务端跟随 302 + 解析落地页 |
| `e.tb.cn` 新版短链 | 落地页是 200 的 HTML，真实地址在 `var url = '...id=xxx'` 里，读响应体后再提取（`TbkShortLinkResolver`） |
| 淘口令 `￥xxx￥` | `taobao.tbk.tpwd.convert`（需「淘口令」权限） |

### 4.5 常见错误对照

| 现象 | 原因 | 处理 |
|---|---|---|
| `code=31 Invalid timestamp` | 时间戳里的空格被编成 `+` | 已修复（改用 `%20` + `URI.create`） |
| `code=26/27` 需要 session | 未完成淘宝账号授权 | 访问 `/prod-api/tbk/auth/start` |
| `code=11 permission-api-package-limit` | 接口权限包未开通 | 到开放平台申请对应权限包 |
| `-4003 请使用升级后的新商品ID` | 商品已进入"新商品ID"灰度，只认字符串ID | 自动降级：跳过商品详情，仍尝试转链 |
| `50001 无结果` | 商品已下架或不是淘宝客商品 | 换商品 |
| 淘口令 `链接不符合规范` | 传入的是原始商品链接 | 必须传转链后的推广链接 |

## 5. 数据表


`sql/tbk.sql` → `tbk_convert_log`，记录每次转换的原始输入、商品ID、通道、IP、成功/失败。

流水写入失败不影响转链主流程（`TbkConvertService#saveLog` 内部已 catch）。

## 6. 注意事项

- 该接口是**匿名公开**的，必须保留 IP 限流；上线后建议再叠加 Nginx 层限流。
- 转链结果有缓存（默认 30 分钟），券信息可能过期，缓存命中时 `notice` 会提示。
- 淘口令/短链有效期有限，用户反馈打不开时重新转一次即可。

## 7. 转链通道（tbk.provider）

通道通过 `@ConditionalOnProperty(prefix = "tbk", name = "provider")` 装配，切换只改配置，业务代码不动。

| provider | 实现类 | 鉴权 | 说明 |
|---|---|---|---|
| `mock` | `MockTbkClient` | 无 | 演示数据 |
| `dataoke` | `DataokeTbkClient` | appKey + appSecret（MD5 签名） | 大淘客，兜底通道 |
| `ztk` | `ZtkTbkClient` | appkey + sid + pid | 折淘客，支持淘口令直接转链 |
| `taobao` | `TbktbClient` | appKey + appSecret（`privilege.get` 需 OAuth session） | **当前生产通道**，走物料搜索升级版 |

### 7.0 官方通道使用限制（2026-11-03 切换后）

- 官方没有「数字商品ID → 新商品ID」的反查接口，只能通过**物料搜索按标题匹配**商品，因此**必须粘贴完整分享文案**（含「商品标题」）；只粘链接/淘口令会提示“需要完整的淘宝分享文案”。
- `privilege.get` / `coupon.convert`（单品券高效转链）权限包尚未批下来；批下来后纯链接也能转，代码会自动优先用标题搜索、再回退 `privilege.get`。
- 结果里的 `itemId` 是联盟新商品ID（每次转链都会变），属于正常现象。
- 官方通道不需要 OAuth 也能完成「标题搜索 + 短链 + 淘口令」；只有 `privilege.get` 需要。

### 7.1 大淘客通道说明（add by CYQ 2026-11-03）

转链优先走「淘系万能解析」，解析不到再回退到「详情 + 高效转链」：

1. `GET /tb-service/parse-content`（**首选**）—— 链接 / 淘口令 / 二合一链接 / 数字商品ID（客户端会拼成商品详情页链接）都能直接返回推广短链、淘口令、标题、主图、当前价和佣金率。大淘客商品库只收录部分商品，走解析接口才不会被"库里没有该商品"挡住。
2. `GET /goods/get-goods-details` —— 解析不到时的回退：数字商品ID 或 新商品ID 查详情，拿 `goodsSign`（新商品ID）；
3. `GET /tb-service/get-privilege-link` —— **必须传 `goodsSign`**，返回 `couponClickUrl`（二合一领券链接，即官方领券页）、`itemUrl`（推广短链）、`tpwd`（淘口令）。

签名：`signRan = MD5("appKey={appKey}&timer={timer}&nonce={nonce}&key={appSecret}")` 转大写，`timer` 为 13 位毫秒时间戳，`nonce` 为 6 位随机数。

已知限制：

| 限制 | 说明 |
|---|---|
| 数字ID不能直接转链 | 联盟已禁止，必须先换 `goodsSign`，客户端已自动处理 |
| 不支持淘口令入参 | 已支持：淘口令会交给「淘系万能解析」，解析成功后直接出推广链接和口令 |
| 短链无商品ID | `m.tb.cn` / `e.tb.cn` 会先跟随跳转，必要时读落地页 HTML 再提取 ID；仍失败时提示复制完整链接或直接粘贴商品ID |
| 商品不在淘客库 | 首选「万能解析」不受商品库覆盖限制；只有解析失败回退到详情接口时，才会提示「该商品暂时无法转链」 |

## 8. 拼多多通道（多多进宝）

拼多多不占用 `tbk.provider`，`TbkConvertService` 会按链接域名自动分流：
`yangkeduo.com` / `pinduoduo.com` / `pdd.cn` → `PddTbkClient`，其余走 `tbk.provider` 配置的淘宝通道。

### 8.1 配置

```yaml
tbk:
  pdd:
    client-id: xxx
    client-secret: xxx
    # 多多进宝-推广管理-推广位ID，形如 123456_789012
    # 必须加引号，否则 YAML 会把下划线当数字分隔符（REDACTED → 44947695319016706）
    pid: "123456_789012"
    gateway: https://gw-api.pinduoduo.com/api/router
    custom-parameters:
```

### 8.2 接口与流程

```
链接（p.pinduoduo.com 短链先跟随跳转）
        ↓ 提取 goods_id / goods_sign
pdd.ddk.goods.detail（标题、主图、拼团价、券、promotion_rate 千分比）
        ↓
pdd.ddk.goods.promotion.url.generate（p_id 推广位，generate_short_url=true）
        ↓
返回 p.pinduoduo.com 短链；拼多多没有淘口令，tpwd 为空
```

签名：`sign = MD5(client_secret + 按参数名排序拼接的 k1v1k2v2... + client_secret)` 转大写。

### 8.3 前置条件（必须）

1. 开放平台应用（client_id）必须用**同一个拼多多账号**登录 [多多进宝](https://jinbao.pinduoduo.com/) 完成 `duoId` 绑定；
2. 在多多进宝「推广管理-推广位」创建推广位，把 `pid` 填到 `tbk.pdd.pid`；
3. **推广位授权备案**：未备案时转链接口返回 `60001 未传入已经授权备案过的相关参数`。
   - 浏览器打开 `GET /tbk/api/pdd/authorize` 拿到授权链接
   - 用**手机**（微信或浏览器）打开该链接，用拼多多账号完成一次授权
   - 再访问 `GET /tbk/api/pdd/status`，返回 `true` 即备案完成

### 8.4 商品 ID 说明

拼多多已下线数字商品ID（`goods_id_list` 返回 `40004/40005`），所有接口必须传 `goods_sign`：

| 接口 | 参数 |
|---|---|
| `pdd.ddk.goods.detail` | 单数 `goods_sign` |
| `pdd.ddk.goods.promotion.url.generate` | 数组 `goods_sign_list` |
| `pdd.ddk.goods.search` | 用 `keyword` + `pid`，返回结果带 `goods_sign` |

链接里没有 `goods_sign`（常见于拼多多 App 的默认分享链接，只有数字 `goods_id`）时：

1. **首选**：把完整分享链接原样作为 `keyword` 调 `pdd.ddk.goods.search`，拼多多会按“终端用户原始链接”解析并返回商品和 `goods_sign`；
   - **必须保留链接末尾的 `share_uin` 等参数**，删掉会报 `40009 该查询不是来自终端用户的原始链接`；
2. 兜底 1：`pdd.goods.detail.get`（商品详情，需单独申请权限，当前返回 `20031`）；
3. 兜底 2：用分享文案里的商品标题调 `pdd.ddk.goods.search` 精确匹配。

### 8.5 已知限制

| 限制 | 说明 |
|---|---|
| 没有淘口令 | 只返回推广短链，`tpwd` 为空，前端“复制淘口令”按钮会自动隐藏 |
| 数字 ID 已下线 | 只认 `goods_sign`，旧链接需要重新从 App 复制 |
| 佣金比例 | PDD 返回 `promotion_rate` 为千分比，已换算成百分比（如 100 → 10%） |

## 9. AI 选品（输入需求 → 跨平台比价打分）

移动端页面：`RuoYi-App/pages/tbk/ai-pick.vue`（工作台「AI 选品」入口，导航标题「AI 帮你挑」）。

因为「高性价比」是主观判断，这里把大模型接进两步：

```
用户一句需求（可带预算/用途/偏好）
        ↓ ① AI 需求解析（SHOPPING_INTENT）
keyword + fallbackKeyword + budget + preferences + category
        ↓ ② 淘宝物料搜索 + 多多进宝搜索（并行，单平台失败自动跳过）
     精确词结果不足 4 条时自动用 fallbackKeyword 再搜一轮
候选商品（标题/券后价/原价/券/销量/店铺，淘宝直接带推广 click_url，拼多多批量生成短链）
        ↓ ③ 规则预排序（预算契合 + 折扣 + 销量）后截断到 pick-max-candidates（每个平台至少保留 2 个）
        ↓ ④ AI 性价比打分（SHOPPING_PICK）
每个候选：score 0-100 / reason / tags / caution + 整体 summary / advice / bestIndex
```

> 选品属于结构化输出场景，两个策略都声明了 `lowLatency()`，会对 Qwen3 系列模型关闭
> `enable_thinking`，把单次打分从 70+ 秒降到 10 秒内；AI 调用本身也有 20/30 秒超时，
> 超时直接降级规则排序，不会让前端等满 60 秒。

AI 任一步失败都会**自动降级为规则排序**，接口仍返回可用结果（`aiUsed=false`）。

### 9.1 选品接口

`POST /tbk/api/pick`（匿名，IP 限流）

```json
{
  "content": "200元以内的降噪蓝牙耳机，通勤用，续航要久一点",
  "budget": 200,
  "maxItems": 8
}
```

- `content` 必填，最长 300 字；
- `budget` 可不填，填了会覆盖 AI 解析出的预算；
- `maxItems` 可选，最终不会超过 `tbk.pick-max-candidates`。

响应 `data`：

```json
{
  "input": "200元以内的降噪蓝牙耳机…",
  "keyword": "降噪蓝牙耳机",
  "budget": 200,
  "preferences": ["通勤", "续航久"],
  "category": "数码",
  "summary": "优先看券后 150 左右、销量过万的两款",
  "advice": "建议先领券再下单；同款在拼多多可能更便宜，注意核对是否官方店。",
  "bestIndex": 1,
  "aiUsed": true,
  "warnings": [],
  "notice": "AI 评分基于平台公开的标题、价格、优惠券和销量，仅供参考…",
  "costMs": 6321,
  "items": [
    {
      "platform": "taobao",
      "platformName": "淘宝",
      "itemId": "xxx",
      "goodsSign": null,
      "title": "商品标题",
      "picUrl": "https://img.alicdn.com/xxx.jpg",
      "originalPrice": 249.00,
      "couponAmount": 60.00,
      "finalPrice": 159.00,
      "couponInfo": "满199减60",
      "sales": 12000,
      "shopName": "xx旗舰店",
      "shortUrl": "https://s.click.taobao.com/xxx",
      "itemUrl": "https://item.taobao.com/item.htm?id=xxx",
      "commissionRate": 12.5,
      "cashbackRate": 90.00,
      "cashbackAmount": 17.89,
      "score": 88,
      "reason": "券后价接近预算下沿，销量高且是官方旗舰店",
      "tags": ["预算内", "券力度大"],
      "caution": "",
      "ruleScore": 86
    }
  ]
}
```

> `commissionRate` 统一为百分数口径：`0.75` 表示 0.75%，`12.5` 表示 12.5%
> （淘宝 `income_rate`、拼多多 `promotion_rate ÷ 10` 均已换算成百分数）。
> 预估返利由后端按 `tbk.cashback-rate` 算好放在 `cashbackAmount` 里
> （= 券后价 × `commissionRate` ÷ 100 × `cashbackRate` ÷ 100，四舍五入 2 位），
> 前端直接展示，不要在前端再乘比例——否则改配置时前后端口径会不一致
> （add by CYQ 2026-11-14 平台服务费，返现比例 90%）。

### 9.2 候选转链接口

`POST /tbk/api/pick/convert`

把选品结果里的某个候选转成最终可发送的推广链接 / 淘口令（内部复用 `TbkConvertService#convert`，
会再走一次转链缓存与流水记录）。

```json
{
  "platform": "taobao",
  "itemId": "xxx",
  "goodsSign": null,
  "title": "商品标题",
  "itemUrl": "https://item.taobao.com/item.htm?id=xxx"
}
```

返回结构与 `/tbk/api/convert` 完全一致。拼多多候选请原样传 `goodsSign`，服务端会拼成带
`goods_sign` 的商品页地址交给多多进宝通道。

### 9.3 相关配置

| 配置项 | 说明 |
|---|---|
| `tbk.pick-rate-limit-per-minute` | 选品单 IP 每分钟上限，默认 3（一次选品调用两次大模型） |
| `tbk.pick-search-size` | 每个平台搜索条数，默认 20 |
| `tbk.pick-max-candidates` | 参与 AI 打分的最大候选数，默认 12 |
| `tbk.pick-cache-seconds` | 选品结果缓存秒数，默认 600 |

AI 走 `ai.opencode-go` 配置（与随手记识别相同），新增策略类：
`com.hms.ai.strategy.ShoppingIntentStrategy`、`ShoppingPickStrategy`，
在 `AIChatFactory.StrategyType` 中注册为 `SHOPPING_INTENT` / `SHOPPING_PICK`。

### 9.4 淘宝搜索依赖

淘宝候选用的是 `taobao.tbk.dg.material.optional.upgrade`（物料搜索升级版），
返回的 `publish_info.click_url` 已绑定自己的推广位，**选品阶段不需要逐个转链**，
只有用户点「生成口令」时才走一次完整转链。

如果该接口权限未开通，选品会自动只返回拼多多结果，并在 `warnings` 里给出提示。

## 10. 美团联盟（自助取链）

### 10.1 配置

```yaml
tbk:
  meituan:
    # 媒体管理里的 Appkey
    appkey: REDACTED
    # 签名密钥
    secret: REDACTED
    # 推广位 SID
    sid: "001"
    # 活动ID：我要推广-活动总览第一列
    act-id:
    # 1-H5 2-美团App deeplink 3-中间页唤起 4-美团小程序 8-优选小程序
    link-type: 1
    gateway: https://openapi.meituan.com/api/generateLink
```

签名规则：

```
sign = MD5(secret + 按参数名升序拼接的 key1value1key2value2... + secret)
```

参与签名的参数为 `appkey / actId / sid / linkType`，**不包含 sign 本身**。

### 10.2 接口

| 接口 | 说明 |
|---|---|
| `GET /tbk/api/meituan/link?actId=xx&linkType=1` | 生成推广链接，`actId` / `linkType` 不传则用配置默认值 |
| `GET /tbk/api/meituan/config` | 查看线上生效的美团配置（appkey 已脱敏） |

响应：

```json
{
  "code": 200,
  "data": {
    "link": "https://runion.meituan.com/url?...",
    "actId": "12345",
    "sid": "001"
  }
}
```

### 10.3 注意事项

1. **actId 必须用当前有效的活动**：老文档里的 `33`（外卖）、`4`（闪购）实测已返回「活动已下线」，
   要到「我要推广 → 活动总览」拿第一列的数字，填到 `tbk.meituan.act-id` 或请求参数 `actId`。
2. **`linkType` 按投放环境选**：
   - `1` H5，适合一般投放；
   - `2` 唤起美团 App；
   - `4` 微信小程序唤起美团小程序（需要先在后台绑定小程序 AppID）；
   - `8` 微信小程序唤起优选小程序。
3. **推广位与媒体绑定**：`sid` 必须属于配置里的 `appkey` 对应媒体。当前 `sid=001` 属于媒体
   `REDACTED`；之前页面看到的 `496009` 属于另一个媒体 `demo-media`，两者不能混用。
4. 美团联盟不对推广者提供全站关键词商品搜索，因此**暂不纳入 AI 选品**；后续如接入活动商品库，
   再按「活动商品 + 关键词过滤 + AI 打分」扩展。
5. **当前定位：只做「外卖领券」入口**，不做选品、不做定时推送、不接订单回调。
   移动端入口在 `RuoYi-App/pages/tbk/index.vue` 的「外卖领券」卡片，
   页面加载时调 `/tbk/api/meituan/config` 探测 `act-id`，未配置则入口置灰。
   美团虽有订单回推接口，但在没有实际成交量的阶段投入订单表/看板不划算，暂不实现。

## 11. 淘宝闪购联盟（本地生活 alsc）

### 11.1 与淘宝客的区别

| 项目 | 淘宝客（现有） | 淘宝闪购 |
|---|---|---|
| 接口族 | `taobao.tbk.*` | `alibaba.alsc.union.*` |
| 推广位 | `mm_xxx_xxx_xxx` | `alsc_xxx_xxx_xxx` |
| 权限包 | 淘宝客权限 | **淘宝闪购联盟权限包（需单独申请）** |
| 业务 | 普通商品 / 天猫 | 外卖、闪购、即时零售、本地生活 |

> 实测当前 appKey `35490624` 调闪购接口返回
> `11 Insufficient isv permissions / isv.permission-api-package-limit`，
> 说明闪购权限包还没开通。

### 11.2 配置

```yaml
tbk:
  alsc:
    # 闪购推广位 pid，格式 alsc_xxx_xxx_xxx
    pid:
    # 默认活动ID
    activity-id: "12628"
    # 三方会员ID（可选）
    sid:
```

### 11.3 接口

| 接口 | 说明 |
|---|---|
| `GET /tbk/api/alsc/link?activityId=12628&sid=xxx` | 闪购官方活动转链 |
| `GET /tbk/api/alsc/config` | 查看闪购配置 |

返回 `AlscActivityLink`，包含：

- `h5Url` / `h5ShortLink`：H5 推广长短链
- `wxAppId` / `wxPath`：微信小程序 path（可直接给小程序跳转）
- `taobaoWord`：淘口令
- `elemeWord`：闪购/饿了么口令
- `deepLink`：App 唤起链接

### 11.4 常用活动ID（以活动总览实际为准）

| 活动 | 活动ID |
|---|---|
| 淘宝闪购（领最高 15 元红包） | 12628 |
| 淘宝闪购-爆品好价 | 12807 |
| 来闪购一下 可赢免单 | 12688 |
| 闪购品牌日 | 12698 |
| 闪购零售品牌日 | 12855 |
| 闪购天天领红包 | 12819 |

### 11.5 单品转链（可按菜品）

闪购还提供单品接口 `alibaba.alsc.union.eleme.promotion.itempromotion.get`：

- `biz_type=hot_item`：爆品券（餐饮套餐，如「鲜蔬乌冬面套餐 5 折」）
- `biz_type=union_item`：即时配送商品

与活动转链不同，单品接口可以拿到具体商品名称、原价/售价、佣金、适用门店数和城市数，
适合做「今天午饭吃这个」的精确推荐；但商品ID需要从闪购物料接口获取，且受城市/门店库存限制。
该接口当前同样需要闪购权限包，代码层预留，等权限下来再补。

## 12. 联盟收益 PC 看板

### 12.1 组成

- 订单表：`tbk_order`（`sql/tbk_order.sql`，按 `platform + order_id` 唯一）
- 同步服务：`TbkOrderService`（淘宝：大淘客订单查询 / 官方接口二选一；拼多多：多多进宝）
- 定时任务：`tbkOrderSyncTask.excute()`，默认每 30 分钟回看 170 分钟
- PC 页面：`RuoYi-Vue3/src/views/tbk/order/index.vue`，菜单「联盟收益 → 订单明细」

### 12.2 接口

| 接口 | 说明 |
|---|---|
| `GET /tbk/order/list` | 订单分页（platform / keyword / beginTime / endTime） |
| `GET /tbk/order/stats` | 今日、本月、累计订单数与佣金，附平台拆分 |
| `POST /tbk/order/sync` | 手动同步；不传 `days` 按「更新时间」回看最近几小时，传 `{"days":7}` 按「付款时间」补最近 7 天 |

### 12.3 权限

| 权限标识 | 说明 |
|---|---|
| `main:tbk:order:list` | 查看订单与收益 |
| `main:tbk:order:sync` | 手动同步 |

菜单 SQL 已内置，挂在顶级目录「联盟收益」下，默认授权角色 100/103。

### 12.4 同步规则

- 淘宝（默认 `tbk.order-sync-taobao-provider: dataoke`）：
  `GET https://openapi.dataoke.com/api/tb-service/get-order-details`，签名 `signRan = MD5(appKey&timer&nonce&key)`。
  定时任务按 `queryType=4`（更新时间）回看 170 分钟，手动补数据按 `queryType=2`（付款时间）回看 N 天；
  该接口单次时间跨度不超过 **3 小时**（大促 20 分钟），代码按 **3 小时** 切片，最多可查近 3 个月。
  **注意**：大淘客订单查询返回的是授权账号下的联盟订单，官方转链产生的订单同样能查到，
  因此转链通道可以继续用官方 `provider: taobao`。
- 淘宝（可选 `tbk.order-sync-taobao-provider: taobao`）：`taobao.tbk.order.details.get`，
  邀请制接口，未开通权限包时返回 `code=11`，按 2 小时切片。保留该通道用于权限开通后切回。
- 拼多多：`pdd.ddk.order.list.increment.get`，按 `update_time` 增量拉取；
  该接口要求单次时间跨度不超过 24 小时，代码按 **23 小时** 切片。
- 写入按 `platform + order_id` **幂等 upsert**，状态变化（付款→收货→结算→退款）会覆盖更新。
- 金额统一存「元」，拼多多的「分」已换算。

PC 页面按钮：

| 按钮 | 范围 | 说明 |
|---|---|---|
| 同步最近 7 天 | 近 7 天 | 日常补数据；大淘客按 3 小时切片约 56 次请求，拼多多约 8 次 |

> PC 端同步接口超时单独设为 300 秒（`src/api/tbk/order.js`），
> 否则默认 10 秒会在批量补同步时被前端提前中断。


### 12.5 数据源说明与限制

**淘宝联盟官方订单接口 `taobao.tbk.order.details.get` 是邀请制**：应用未拿到权限包时返回
`code=11 / isv.permission-api-package-limit`，而且用户授权（session）不能替代权限包。
联盟自 2024-11-12 起暂停后台自申请，只能走工单，门槛为「日均引流>5w、日均GMV>10w、风控扣分<18」。

因此淘宝订单默认改走**大淘客订单查询**（`DataokeOrderClient`），实测：
- 用同一个推广位账号即可查到联盟订单，**官方转链产生的订单也能查到**；
- 订单生成后约 5 分钟内可见；
- 字段与联盟订单接口基本一致（`trade_id`/`alipay_total_price`/`pub_share_pre_fee`/`pub_share_fee`/`tk_status` 等）。

限制与风险：
- 单次时间跨度 ≤3 小时，最多查近 3 个月；
- 大淘客有默认调用量额度，不够需联系其客服提量；被限流时 `taobao.error` 会记录错误，不影响拼多多；
- 依赖第三方账号授权与平台策略，大淘客若收紧「仅自家转链订单」该通道会失效，
  这时把 `tbk.order-sync-taobao-provider` 改回 `taobao` 即可回到官方接口（需先拿到权限包）；
- 返回报文里有主订单和赠品子订单（0 元 0 佣金），都会入库，收益统计按佣金汇总不受影响。

定时任务默认 **暂停**（`sys_job.status='1'`），确认数据正常后，在「系统监控 → 定时任务」里启动。

### 12.6 用户归因（返现基础）

订单表已增加：

| 字段 | 说明 |
|---|---|
| `user_id` | HMS 用户ID，登录态下由后端从 token 解析 |
| `external_user` | 平台侧归因标识（拼多多 `custom_parameters.uid` / 淘宝 `special_id` / 闪购 `sid`） |
| `user_name` | 不落库，列表查询时 `left join sys_user` 取 |

归因规则：

1. **登录用户**：移动端请求不再强制 `isToken:false`，有 token 时后端用 `LoginUser.userId`，客户端传的 `userKey` 会被忽略；
2. **匿名用户**：客户端生成稳定 `userKey`，后端加 `anon:` 前缀存到 `external_user`，只用于统计，**不建议参与返现**；
3. **拼多多**：转链时写入 `custom_parameters = {"uid":"<用户ID>"}`，订单同步时从 `custom_parameters` 反解出 `uid`；
4. **淘宝**：因为订单查询接口未开通，且淘宝客物料搜索没有可靠的 per-user 归因参数，暂不注入用户标识；
5. **淘宝闪购**：`sid` 本身就是三方会员ID，可直接传用户标识（等权限开通）。

缓存注意：拼多多推广链接现在带用户归因，转链缓存和 AI 选品缓存都已把 `userKey` 纳入 key，避免把 A 的推广链接返回给 B。

**拼多多归因的前置条件（重要）**：拼多多要求「`pid` + `custom_parameters`」**每组都单独备案**，
未备案时带 `custom_parameters` 调转链接口会返回 `60001`。所以配置里：

```yaml
tbk:
  pdd:
    user-attribution-enabled: false
```

默认关闭，链接不带用户标识，转链不受影响（代码在 60001 时会自动回退无归因链接）。
后续做返现时，需要先为每个用户完成一次授权备案：

- 微信小程序：`pdd.ddk.rp.prom.url.generate`（`channel_type=10`、`generate_we_app=true`），
  拿 `app_id/page_path` 让用户跳转拼多多授权；
- H5/App：`pdd.ddk.goods.promotion.url.generate` 加 `generate_authority_url=true`；
- 备案状态查询：`pdd.ddk.member.authority.query`（传 `pid` + `custom_parameters`）。

备案完成后把 `user-attribution-enabled` 改成 `true`，订单同步就能从
`custom_parameters.uid` 反解出用户，`tbk_order.user_id` 才会有值。

> 已经产生的历史订单（`custom_parameters` 为空）无法事后归因，只能从开启之后的新订单开始统计。

#### 零用户操作方案：链接台账 + 近似匹配

拼多多/淘宝的订单接口**不返回**生成的短链或链接 code，所以没有「拿链接直接查订单」的接口。
零用户操作下，只能反过来做：

1. 每次生成推广链接（转链 / AI 选品）写一条 `tbk_link` 台账：
   `user_id / anon_key / platform / item_id / goods_sign / pid / short_url / create_time`；
2. 订单同步后，用订单里的 `goods_id / goods_sign + p_id + 付款时间` 去匹配台账：
   - 付款前 **7 天**内、同商品、同平台；
   - 只有 1 个用户生成过 → `match_type=approx`，归到该用户；
   - 多个用户生成过 → `match_type=ambiguous`，不强行归属；
   - 没有记录 → `match_type=none`。
3. 按配置比例计算应返现：

```yaml
tbk:
  cashback-rate: 90              # 占佣金百分比（平台抽 10% 服务费，返给用户 90%）
  attribution-window-days: 7     # 匹配时间窗
```

订单表新增：

| 字段 | 说明 |
|---|---|
| `link_id` | 命中的链接台账 |
| `match_type` | deterministic / approx / ambiguous / anon / none |
| `cashback_rate` | 返现比例 |
| `cashback_amount` | 应返现金额 |
| `cashback_status` | estimated / settled / invalid / ambiguous / unattributed / unpaid |

> 这是「近似归属」，不是平台级精确归因。要 100% 精确，只有两条路：
> 拼多多 `pid + custom_parameters` 逐组备案（需要用户授权），
> 或接第三方 CPS 聚合平台（由对方处理备案，用他们的 `sid` 跟单）。

后续做返现时，建议只对 `settled_commission`（已结算）且 `cashback_status=settled` 的订单结算；
退款/风控订单（`invalid`）不返，`ambiguous` 的订单先人工确认。







