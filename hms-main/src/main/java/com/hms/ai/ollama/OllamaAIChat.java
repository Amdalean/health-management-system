package com.hms.ai.ollama;

import com.hms.ai.core.AbstractOpenAiChat;
import com.hms.ai.strategy.AIResponseStrategy;
import com.hms.common.config.OllamaConfig;
import com.hms.common.utils.StringUtils;

/**
 * Ollama AI对话实现类
 * 集成本地Ollama服务进行AI对话
 *
 * add by CYQ 2026年4月6日
 * update by CYQ 2026-09-21 AI 模块重构：归位至 ai.ollama，支持配置读取
 * update by CYQ 2026-11-15 统一协议：改为继承 AbstractOpenAiChat，走 Ollama 的
 *        OpenAI 兼容端点 /v1/chat/completions，与云端 Qwen 共用同一套请求构造与响应解析
 */
public class OllamaAIChat extends AbstractOpenAiChat {

    /**
     * Ollama服务主机地址（默认值，可被配置覆盖）
     */
    private static final String DEFAULT_HOST = "http://localhost:11434";

    /**
     * 默认使用的模型名称（默认值，可被配置覆盖）
     */
    private static final String DEFAULT_MODEL = "qwen3.5:9b";

    /**
     * Ollama服务地址
     */
    private final String ollamaHost;

    /**
     * 模型名称
     */
    private final String modelName;

    public OllamaAIChat(AIResponseStrategy strategy)
    {
        this(strategy, null, null);
    }

    public OllamaAIChat(AIResponseStrategy strategy, String ollamaHost, String modelName)
    {
        super(strategy);
        this.ollamaHost = StringUtils.isNotEmpty(ollamaHost) ? ollamaHost
                : (StringUtils.isNotEmpty(OllamaConfig.getHost()) ? OllamaConfig.getHost() : DEFAULT_HOST);
        this.modelName = StringUtils.isNotEmpty(modelName) ? modelName
                : (StringUtils.isNotEmpty(OllamaConfig.getModel()) ? OllamaConfig.getModel() : DEFAULT_MODEL);
    }

    /**
     * Ollama 的 OpenAI 兼容端点，与云端 Qwen 使用同一路径规范
     */
    @Override
    protected String getApiUrl()
    {
        return ollamaHost + "/v1/chat/completions";
    }

    @Override
    protected String getModel()
    {
        return modelName;
    }

    /**
     * 本地 Ollama 不校验密钥，此处返回占位值以满足统一鉴权头
     */
    @Override
    protected String getApiKey()
    {
        return "ollama";
    }

    @Override
    protected String getProvider()
    {
        return "ollama";
    }
}
