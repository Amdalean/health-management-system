package com.hms.ai.template;

import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.annotation.Log;
import com.hms.common.config.QwenConfig;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.http.HttpUtils;
import com.hms.common.utils.spring.SpringUtils;
import com.hms.main.integration.ai.AiChatIntegrationStrategy;
import com.hms.main.integration.ai.AiChatRequest;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * AI对话抽象模板类 - 使用模板方法模式
 * 包含AI连接等通用配置处理
 */
public abstract class AbstractAIChatTemplate {
    
    protected AIResponseStrategy strategy;
    private static final Logger log = LoggerFactory.getLogger(AbstractAIChatTemplate.class);

    public AbstractAIChatTemplate(AIResponseStrategy strategy) {
        this.strategy = strategy;
    }
    protected Map<String, Object> context;
    /**
     * 模板方法：定义AI对话的通用流程
     */

    @Log(title = "AI对话", businessType = BusinessType.OTHER)
    public final String chat(String input, Map<String, Object> context) {
        this.context = context;
        // 预处理步骤 - 由子类实现
        String processedInput = preprocessInput(input);
        
        // 使用策略处理响应 - 由策略实现
        String response = strategy.processResponse(processedInput, context);
        
        // 调用AI服务 - 通用处理
        String aiResponse = callAIService(response);
        
        // 后处理步骤 - 由子类实现
        String finalResponse = postprocessResponse(aiResponse);
        
        return finalResponse;
    }

    /**
     * 统一走 AI 集成策略发送 HTTP 请求（自动记录系统集成日志）
     * <p>
     * 策略为 Spring Bean，非 Spring 上下文（如直接运行 main 方法）降级为直连，
     * 保证调用可用性不受影响，此时不记录集成日志。
     *
     * add by CYQ 2026-10-19 AI 调用接入系统集成日志
     *
     * @param request AI 请求参数
     * @return 原始响应报文
     */
    protected String sendByIntegration(AiChatRequest request) throws Exception {
        AiChatIntegrationStrategy strategy = resolveIntegrationStrategy();
        if (strategy == null) {
            log.warn("未获取到 AI 集成策略（当前非 Spring 上下文），本次调用不记录集成日志");
            return HttpUtils.dopost_OKHttp(request.getUrl(), request.getBody(), request.getHeaders());
        }
        return strategy.execute(request).getRaw();
    }

    /**
     * 获取 AI 集成策略 Bean，容器未就绪时返回 null
     */
    private AiChatIntegrationStrategy resolveIntegrationStrategy() {
        try {
            return SpringUtils.getBean(AiChatIntegrationStrategy.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从上下文解析单据标识，便于按业务单据检索集成日志（取上下文 billId / sessionId）
     */
    protected String resolveBillId() {
        if (context == null) {
            return null;
        }
        Object billId = context.get("billId");
        if (billId == null) {
            billId = context.get("sessionId");
        }
        return billId == null ? null : String.valueOf(billId);
    }

    /**
     * 预处理输入 - 由子类实现
     */
    protected abstract String preprocessInput(String input);

    /**
     * 后处理响应 - 由子类实现
     */
    protected abstract String postprocessResponse(String response);

    /**
     * 获取系统提示词 - 由子类实现
     * @return 系统提示词
     */
    protected abstract String getSystemPrompt();
    /**
     * 调用AI服务 - 通用方法
     * @param input 用户输入
     * @return AI响应
     */
    protected abstract String callAIService(String input);
}