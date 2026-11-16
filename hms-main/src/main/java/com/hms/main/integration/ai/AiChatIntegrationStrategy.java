package com.hms.main.integration.ai;

import com.hms.common.annotation.IntegrationLog;
import com.hms.common.integration.AbstractIntegrationStrategy;
import com.hms.common.utils.DateUtils;
import com.hms.common.utils.StringUtils;
import com.hms.common.utils.http.HttpUtils;
import cn.hutool.core.util.RandomUtil;

/**
 * AI 对话集成策略
 * <p>
 * 所有 AI 大模型调用（Qwen、OpenCode Go、Ollama 等）统一走 {@link #execute(AiChatRequest)}，
 * 由系统集成日志切面自动记录服务商、模型、请求/响应报文、开始时间、耗时、执行状态等。
 * 类上标注 {@link IntegrationLog} 已自动注册为 Spring Bean，无需额外 {@code @Component}。
 * <p>
 * 接收方格式为 {@code 服务商/模型}，如 {@code opencode-go/deepseek-v4.1-flash}。
 *
 * add by CYQ 2026-10-19 AI 调用接入系统集成日志
 */
@IntegrationLog(title = "AI对话", bizType = "AI", sender = "HMS")
public class AiChatIntegrationStrategy extends AbstractIntegrationStrategy<AiChatRequest, AiChatResponse>
{
    /** 兜底单据标识前缀 */
    private static final String BILL_ID_PREFIX = "AI";

    @Override
    protected AiChatResponse doSend(AiChatRequest request) throws Exception
    {
        String raw = HttpUtils.dopost_OKHttp(request.getUrl(), request.getBody(), request.getHeaders());
        return new AiChatResponse(raw);
    }

    /**
     * 单据标识：优先取业务单号，缺失时兜底生成 {@code AI-yyyyMMddHHmmss-xxxx}，保证日志可检索
     *
     * add by CYQ 2026-10-19 AI 调用接入系统集成日志
     */
    @Override
    public String resolveBillId(AiChatRequest request)
    {
        if (StringUtils.isNotEmpty(request.getBillId()))
        {
            return request.getBillId();
        }
        return BILL_ID_PREFIX + "-" + DateUtils.dateTimeNow(DateUtils.YYYYMMDDHHMMSS) + "-" + RandomUtil.randomString(4);
    }

    @Override
    public String resolveReceiver(AiChatRequest request)
    {
        if (StringUtils.isEmpty(request.getProvider()))
        {
            return request.getModel();
        }
        return StringUtils.isEmpty(request.getModel())
                ? request.getProvider()
                : request.getProvider() + "/" + request.getModel();
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
