package com.hms.ai.template;

import com.alibaba.fastjson2.JSONObject;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.utils.http.HttpUtils;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

/**
 * Ollama AI对话实现类
 * 集成本地Ollama服务进行AI对话
 * 使用不同策略处理不同场景（如健康建议、报表输出、财务分析等）
 *
 * add by CYQ 2026年4月6日
 */
@Log4j2
public class OllamaAIChat extends AbstractAIChatTemplate {

    /**
     * Ollama服务主机地址
     */
    private static final String DEFAULT_HOST = "http://localhost:11434";

    /**
     * 默认使用的模型名称
     */
    private static final String DEFAULT_MODEL = "qwen3.5:9b";

    /**
     * Ollama服务地址
     */
    private String ollamaHost = DEFAULT_HOST;

    /**
     * 模型名称
     */
    private String modelName = DEFAULT_MODEL;

    public OllamaAIChat(AIResponseStrategy strategy) {
        super(strategy);
    }

    public OllamaAIChat(AIResponseStrategy strategy, String ollamaHost, String modelName) {
        super(strategy);
        if (ollamaHost != null && !ollamaHost.isEmpty()) {
            this.ollamaHost = ollamaHost;
        }
        if (modelName != null && !modelName.isEmpty()) {
            this.modelName = modelName;
        }
    }

    @Override
    protected String preprocessInput(String input) {
        // Ollama特有的预处理逻辑
        // 可以根据策略类型添加特定的预处理步骤
        log.info("Ollama预处理输入: {}", input);
        return input;
    }

    @Override
    protected String postprocessResponse(String response) {
        // Ollama特有的后处理逻辑
        log.info("Ollama后处理响应完成");
        return response;
    }

    @Override
    protected String getSystemPrompt() {
        // 根据当前策略返回相应的系统提示词
        // 不同策略（健康建议、报表输出、财务分析、成绩分析等）可以有不同的系统提示词
        return strategy.getStrategyName();
    }

    /**
     * 调用Ollama AI服务
     * @param input 用户输入
     * @return AI响应
     */
    @Override
    protected String callAIService(String input) {
        try {
            log.info("调用Ollama AI服务，模型: {}, 输入: {}", modelName, input);
            
            // 构建请求JSON
            JSONObject requestJson = new JSONObject();
            requestJson.put("model", modelName);
            requestJson.put("prompt", input);
            requestJson.put("stream", false);
            
            // 添加系统提示词
            String systemPrompt = getSystemPrompt();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                requestJson.put("system", systemPrompt);
            }
            
            log.info("Ollama请求内容: {}", requestJson.toString());
            
            // 调用Ollama API
            String url = ollamaHost + "/api/generate";
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            
            String response = HttpUtils.dopost_OKHttp(url, requestJson.toString(), headers);
            
            log.info("Ollama原始响应: {}", response);
            
            // 解析响应
            return parseOllamaResponse(response);
            
        } catch (Exception e) {
            log.error("调用Ollama AI服务异常", e);
            return "Ollama AI服务调用失败: " + e.getMessage();
        }
    }

    /**
     * 解析Ollama响应
     * @param response 原始响应JSON
     * @return 提取的文本内容
     */
    private String parseOllamaResponse(String response) {
        try {
            JSONObject json = JSONObject.parseObject(response);
            if (json.containsKey("response")) {
                return json.getString("response");
            } else if (json.containsKey("message")) {
                return json.getString("message");
            } else {
                log.warn("Ollama响应格式异常: {}", response);
                return response;
            }
        } catch (Exception e) {
            log.error("解析Ollama响应失败", e);
            return response;
        }
    }
}
