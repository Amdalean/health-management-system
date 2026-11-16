# 系统集成日志接口

> add by CYQ 2026-09-14 系统集成日志框架

系统集成日志用于记录所有对外集成（邮件、HTTP、MQ 等）的调用明细，字段包括发送方、接收方、服务器标识、单据标识、发送报文、响应报文、开始时间、结束时间、耗时及执行状态。

## 1. 查询集成日志列表

- **URL**: `GET /monitor/integrationlog/list`
- **权限**: `monitor:integrationlog:list`
- **请求参数**（分页 + 过滤，均可选）:

| 参数 | 说明 |
| --- | --- |
| pageNum / pageSize | 分页参数 |
| title | 集成节点名称（模糊） |
| bizType | 业务类型/渠道（精确，如 EMAIL） |
| sender | 发送方（模糊） |
| receiver | 接收方（模糊） |
| serverId | 服务器标识（模糊） |
| billId | 单据标识（模糊） |
| status | 状态（0成功 1失败） |
| params.beginTime / params.endTime | 开始时间范围 |

- **返回**: `TableDataInfo`

## 2. 获取集成日志详细

- **URL**: `GET /monitor/integrationlog/{logId}`
- **权限**: `monitor:integrationlog:query`
- **返回**: `AjaxResult`，data 为 `HmsIntegrationLog`

## 3. 导出集成日志

- **URL**: `POST /monitor/integrationlog/export`
- **权限**: `monitor:integrationlog:export`

## 4. 删除集成日志

- **URL**: `DELETE /monitor/integrationlog/{logIds}`
- **权限**: `monitor:integrationlog:remove`
- **参数**: 逗号分隔的日志 ID 数组

## 5. 清空集成日志

- **URL**: `DELETE /monitor/integrationlog/clean`
- **权限**: `monitor:integrationlog:remove`

## 接入方式

### 一、策略模式 + 模版方法（推荐）

```java
@IntegrationLog(title = "邮件通知", bizType = "EMAIL", sender = "HMS")
public class EmailIntegrationStrategy extends AbstractIntegrationStrategy<EmailSendRequest, EmailSendResponse>
{
    @Override
    protected EmailSendResponse doSend(EmailSendRequest request) throws Exception
    {
        // 真正的对接逻辑
        return new EmailSendResponse();
    }

    @Override
    public String resolveBillId(EmailSendRequest request)
    {
        return request.getBillId();
    }

    @Override
    public String resolveReceiver(EmailSendRequest request)
    {
        return String.join(",", request.getTos());
    }
}
```

调用方直接注入策略并按具体类型执行 `execute`，日志由切面自动记录：

```java
@Autowired
private EmailIntegrationStrategy emailIntegrationStrategy;

emailIntegrationStrategy.execute(new EmailSendRequest(tos, subject, content, billId));
```

> `@IntegrationLog` 已通过元注解集成 `@Component`，标注该注解的类会自动注册为 Spring Bean，无需再写 `@Component`。
> 框架已开启 CGLIB 代理（`proxyTargetClass = true`），因此可按策略具体类型注入。

### 二、类上直接标注

在任意 Spring Bean 类上标注 `@IntegrationLog`，切面会拦截该类的所有 public 方法按其入参/返回值自动记录日志。

### 三、AI 大模型调用（HTTP 类集成示例）

非 Spring Bean 的调用方（如 `AIChatFactory` 用 `new` 创建的 AI 对话实例）可把 HTTP 调用收敛到一个策略 Bean，
再由策略的 `execute` 触发切面记录，参见 `com.hms.main.integration.ai.AiChatIntegrationStrategy`：

```java
@IntegrationLog(title = "AI对话", bizType = "AI", sender = "HMS")
public class AiChatIntegrationStrategy extends AbstractIntegrationStrategy<AiChatRequest, AiChatResponse>
{
    @Override
    protected AiChatResponse doSend(AiChatRequest request) throws Exception
    {
        return new AiChatResponse(HttpUtils.dopost_OKHttp(request.getUrl(), request.getBody(), request.getHeaders()));
    }

    @Override
    public String resolveReceiver(AiChatRequest request)
    {
        return request.getProvider() + "/" + request.getModel();
    }

    @Override
    public Object resolveSendMessage(AiChatRequest request)
    {
        return request.getBody();
    }

    @Override
    public Object resolveResponseMessage(AiChatResponse response)
    {
        return response == null ? null : response.getRaw();
    }
}
```

调用方通过 `SpringUtils.getBean(...)` 取策略并 `execute`，即可自动落集成日志。

## 字段说明

| 字段 | 含义 |
| --- | --- |
| title | 集成节点名称 |
| bizType | 业务类型/渠道 |
| sender | 发送方 |
| receiver | 接收方 |
| serverId | 服务器标识（未配置时自动取本机 IP） |
| billId | 单据标识 |
| sendMessage | 发送报文 |
| responseMessage | 响应报文 |
| startTime / endTime | 开始时间 / 结束时间 |
| costTime | 耗时（毫秒） |
| status | 0成功 1失败 |
| errorMsg | 异常信息 |
