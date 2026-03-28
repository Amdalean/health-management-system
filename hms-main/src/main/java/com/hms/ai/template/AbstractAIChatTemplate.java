package com.hms.ai.template;

import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.annotation.Log;
import com.hms.common.config.QwenConfig;
import com.hms.common.enums.BusinessType;
import com.hms.common.exception.base.BaseException;
import com.hms.common.utils.http.HttpUtils;
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
    Map<String, Object> context;
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