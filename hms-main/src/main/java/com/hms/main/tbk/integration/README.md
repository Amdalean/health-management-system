# 淘宝 / 拼多多接口策略类与集成日志

本目录是淘宝客（tbk）模块对「电商平台」的接口调用策略层：所有对淘宝开放平台（TOP）
和拼多多多多进宝（DDK）的 HTTP 接口调用都从这里发起，不再散落在各业务 Client/Service 里。

> add by CYQ 2026-11-10 淘宝/拼多多接口策略类与集成日志

## 1. 为什么单独抽策略类

| 问题 | 策略类怎么解决 |
|---|---|
| 转链、选品、订单同步、推广位查询各自拼参数、各自签名，改签名规则要改多处 | 公共参数 + 签名只在 `TbkApiSigner` + 策略类里维护一份 |
| 线上排查「这次转链到底请求了什么、接口返回了什么、耗时多少」只能翻应用日志 | 类上 `@IntegrationLog`，切面自动把请求/响应/耗时/结果落库 `hms_integration_log` |
| 业务 Client 里混着 HTTP、签名、错误码映射，不好单测 | 策略类只做协议，Client 只做业务参数组装与响应解析 |

这套写法沿用项目已有的系统集成框架（`hms-common` 的
`IntegrationStrategy` / `AbstractIntegrationStrategy` + `@IntegrationLog`，
切面实现见 `hms-framework` 的 `IntegrationLogAspect`，邮件、AI 对话策略也是同一套）。

## 2. 类结构

```
hms-main/src/main/java/com/hms/main/tbk/integration/
├── TbkApiSigner.java            公共签名 / 时间戳 / query string / 脱敏工具
├── TaobaoTopApiRequest.java     淘宝 TOP 请求（method + biz + session）
├── TaobaoTopApiStrategy.java    淘宝 TOP 接口策略（GET，自动记录集成日志）
├── PddDdkApiRequest.java        拼多多 DDK 请求（type + biz）
└── PddDdkApiStrategy.java       拼多多 DDK 接口策略（POST 表单，自动记录集成日志）
```

调用链：

```
TbkConvertService / TbkPickService / TbkOrderService / TbkPickController ...
        │
        ├─ TopClient.execute(method, biz, session)      // 只做响应解析 + 错误映射
        │       └─ TaobaoTopApiStrategy.execute(req)    // 公共参数 + 签名 + HTTP + 日志
        │
        └─ PddTbkClient.call(type, biz)                 // 只做响应解析 + 错误映射
                └─ PddDdkApiStrategy.execute(req)       // 公共参数 + 签名 + HTTP + 日志
```

`TopClient.sign()` / `TopClient.toQueryString()` / `PddTbkClient.sign()` 三个静态方法保留，
内部委托给 `TbkApiSigner`，老调用和单元测试不用改。

## 3. 集成日志落库规则

类注解：

```java
@IntegrationLog(title = "淘宝TOP接口", bizType = "TAOBAO_TOP", sender = "HMS", receiver = "淘宝开放平台")
@IntegrationLog(title = "拼多多DDK接口", bizType = "PDD_DDK", sender = "HMS", receiver = "拼多多开放平台")
```

切面异步写入 `hms_integration_log`（建表脚本 `sql/table/hms_integration_log.sql`）：

| 字段 | 来源 |
|---|---|
| `title` / `biz_type` / `sender` / `receiver` | 策略类 `@IntegrationLog` 注解 |
| `bill_id` | `resolveBillId()`：`item_id`、`num_iid`、`goods_sign`、`order_sn`、`p_id` 等 |
| `send_message` | `resolveSendMessage()`：接口名 + 业务参数 + 脱敏后的 session/client_id |
| `response_message` | `resolveResponseMessage()`：接口响应体（切面按 64KB 截断） |
| `status` / `error_msg` | 成功 `0`；抛异常 `1` 并记录异常信息 |
| `start_time` / `end_time` / `cost_time` | 切面自动计时（毫秒） |
| `oper_name` | 有登录上下文取用户名，定时任务为 `system` |
| `create_time` | 落库时 `sysdate()` |

查询入口：PC 端「监控 → 集成日志」（`/monitor/integrationlog/list`，
权限 `monitor:integrationlog:list`），可按 `bizType` = `TAOBAO_TOP` / `PDD_DDK` 过滤。

**脱敏约定**：

- 淘宝 OAuth `session` 只保留前 4 后 4 位（`abcd****wxyz`），app_secret 不进入日志；
- 拼多多 `client_id` 同样脱敏，client_secret 不进入日志；
- 响应报文按切面统一 65535 字符截断。

## 4. 新增一个接口 / 新平台怎么做

1. 新建请求对象（只放接口名 + 业务参数）；
2. 新建策略类，继承 `AbstractIntegrationStrategy<请求, 响应>`，类上标 `@IntegrationLog`；
3. `doSend()` 里做公共参数、签名、HTTP、空响应兜底，异常要往外抛（切面靠异常判定失败）；
4. 覆盖 `resolveBillId()` / `resolveSendMessage()` / `resolveResponseMessage()` 决定日志内容；
5. 原 Client 只保留业务参数组装和响应解析，调用策略类的 `execute()`；
6. 单测放在 `hms-admin/src/test/java/com/hms/test/`，至少覆盖参数组装、签名可复算、日志脱敏。

## 5. 当前覆盖范围

已纳入策略层：

- 淘宝开放平台 TOP：`taobao.tbk.*`、`taobao.time.get` 等所有走 `TopClient.execute()` 的接口；
- 拼多多多多进宝 DDK：`pdd.ddk.*`、`pdd.goods.*` 等所有走 `PddTbkClient.call()` 的接口。

暂未纳入（仍是各自 Client 里直连，后续可按同样套路迁入）：

- 淘宝 OAuth 换 token（`TaobaoAuthService`）；
- 淘宝短链解析（`TbkShortLinkResolver`，直接请求 m.tb.cn / e.tb.cn 页面）；
- 大淘客 `DataokeTbkClient`、折淘客 `ZtkTbkClient`、美团 `MeituanTbkClient`、闪购 `AlscTbkClient`。

> 注意：联盟订单同步会按时间窗 + 分页反复调用接口，日志量会比转链大很多，
> 排查时建议按 `biz_type` + 时间范围过滤。
